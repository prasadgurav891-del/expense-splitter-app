// Import the required Firebase Functions and Admin SDK
const { onRequest } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");
const logger = require("firebase-functions/logger");

// Initialize Firebase Admin SDK
admin.initializeApp();

// Cloud Function to send push notifications
exports.sendNotification = onRequest((request, response) => {
    // Get the FCM token and message details from the request body
    const token = request.body.token; // Device FCM token
    const message = {
        notification: {
            title: request.body.title || "Default Title", // Use request body title or default
            body: request.body.body || "Default message body" // Use request body body or default
        },
        token: token, // Target device token
    };

    // Send the notification via Firebase Messaging
    admin.messaging().send(message)
        .then((result) => {
            // Success - Respond with success message
            logger.info("Notification sent successfully: " + result);
            response.status(200).send("Notification sent successfully: " + result);
        })
        .catch((error) => {
            // Error - Respond with error message
            logger.error("Error sending notification: " + error);
            response.status(500).send("Error sending notification: " + error);
        });
});
