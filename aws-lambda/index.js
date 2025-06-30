const AWS = require('aws-sdk');

// AWS Services initialisieren
const ses = new AWS.SES({ region: 'eu-central-1' });
const sns = new AWS.SNS({ region: 'eu-central-1' });

exports.handler = async (event, context) => {
    console.log('🚀 LuckyPets Lambda triggered:', JSON.stringify(event, null, 2));

    try {
        // Event von API Gateway parsen
        const body = typeof event.body === 'string' ? JSON.parse(event.body) : event.body;
        const { eventType, shipmentId, customerId, origin, destination, timestamp, correlationId } = body;

        console.log('📨 Processing event:', { eventType, shipmentId, destination });

        // Customer Data (in Produktion: DynamoDB Lookup)
        const customer = {
            email: `awsLuckypets@gmx.de`,
            phone: '+49543252355', // Verified phone für SNS
            name: `Demo Kunde ${(customerId || 'DEMO').slice(-3)}`
        };

        // Notification Templates
        const templates = getNotificationTemplates(eventType, shipmentId, destination);

        // E-Mail via SES senden
        const emailResult = await sendEmail(customer.email, templates.email);
        console.log('📧 Email sent:', emailResult.MessageId);

        // SMS via SNS senden (optional - braucht verified phone)
        // const smsResult = await sendSMS(customer.phone, templates.sms);
        // console.log('📱 SMS sent:', smsResult.MessageId);

        // CloudWatch Metrics loggen
        await logMetrics(eventType, 'success');

        return {
            statusCode: 200,
            headers: {
                'Content-Type': 'application/json',
                'Access-Control-Allow-Origin': '*'
            },
            body: JSON.stringify({
                status: 'success',
                message: 'AWS Lambda executed successfully',
                lambda: {
                    requestId: context.awsRequestId || 'local-test',
                    functionName: context.functionName || 'luckypets-notifications',
                    executionTime: Date.now() - (timestamp || Date.now()),
                    memoryLimit: context.memoryLimitInMB || '128MB'
                },
                notifications: {
                    email: { status: 'sent', recipient: customer.email, messageId: emailResult.MessageId },
                    sms: { status: 'skipped', reason: 'Phone not verified' }
                },
                correlationId
            })
        };

    } catch (error) {
        console.error('❌ Lambda execution failed:', error);
        await logMetrics('error', 'failure');

        return {
            statusCode: 500,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                status: 'error',
                message: error.message,
                correlationId: event.correlationId
            })
        };
    }
};

function getNotificationTemplates(eventType, shipmentId, destination) {
    const templates = {
        'shipment-created': {
            email: {
                subject: '🐾 Ihre LuckyPets Sendung ist unterwegs!',
                body: `Ihre Sendung ${shipmentId} ist unterwegs nach ${destination}! Wir halten Sie auf dem Laufenden.`
            },
            sms: `🐾 LuckyPets: Sendung ${shipmentId} → ${destination}`
        },
        'shipment-scanned': {
            email: {
                subject: '📍 LuckyPets Sendungsupdate',
                body: `Update: Ihre Sendung ${shipmentId} macht Fortschritte Richtung ${destination}.`
            },
            sms: `📍 LuckyPets: ${shipmentId} gescannt → ${destination}`
        },
        'shipment-delivered': {
            email: {
                subject: '🎉 LuckyPets Zustellung erfolgreich!',
                body: `Großartig! Ihre Sendung ${shipmentId} wurde erfolgreich in ${destination} zugestellt!`
            },
            sms: `🎉 LuckyPets: ${shipmentId} zugestellt in ${destination}!`
        }
    };

    return templates[eventType] || templates['shipment-created'];
}

async function sendEmail(recipient, template) {
    const params = {
        Destination: { ToAddresses: [recipient] },
        Message: {
            Body: { Text: { Data: template.body, Charset: 'UTF-8' } },
            Subject: { Data: template.subject, Charset: 'UTF-8' }
        },
        Source: 'awsLuckypets@gmx.de' // Muss verified sein in SES
    };

    return await ses.sendEmail(params).promise();
}

async function sendSMS(phoneNumber, message) {
    const params = {
        PhoneNumber: phoneNumber,
        Message: message
    };

    return await sns.publish(params).promise();
}

async function logMetrics(eventType, status) {
    // CloudWatch Custom Metrics
    const cloudwatch = new AWS.CloudWatch({ region: 'eu-central-1' });

    const params = {
        Namespace: 'LuckyPets/Notifications',
        MetricData: [{
            MetricName: 'ProcessedEvents',
            Dimensions: [
                { Name: 'EventType', Value: eventType },
                { Name: 'Status', Value: status }
            ],
            Value: 1,
            Unit: 'Count',
            Timestamp: new Date()
        }]
    };

    try {
        await cloudwatch.putMetricData(params).promise();
    } catch (error) {
        console.warn('⚠️ CloudWatch metrics failed:', error.message);
    }
}