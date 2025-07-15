# Travel Planner App

## Google Sign-In Configuration Issue

There is currently an issue with Google Sign-In functionality in this app. The error `Unresolved reference: default_web_client_id` occurs because the OAuth client configuration is missing in the Firebase project.

### How to Fix Google Sign-In

1. **Configure OAuth Client in Firebase Console:**
   - Go to the [Firebase Console](https://console.firebase.google.com/)
   - Select your project: `travelplannerapp-5617e`
   - Navigate to Authentication → Sign-in method
   - Enable Google as a sign-in provider
   - Configure the OAuth consent screen

2. **Add SHA-1 Certificate Fingerprint:**
   - In Firebase Console, go to Project Settings
   - Add your app's SHA-1 fingerprint
   - You can get your SHA-1 by running this command in Android Studio terminal:
     ```
     ./gradlew signingReport
     ```

3. **Download Updated google-services.json:**
   - After configuring OAuth, download the updated google-services.json file
   - Replace the existing file in your app directory
   - The new file should contain entries in the `oauth_client` array

4. **Rebuild the App:**
   - Clean and rebuild the project
   - The Google Services plugin will generate the `default_web_client_id` resource

### Temporary Workaround

As a temporary workaround, Google Sign-In has been disabled in the app. Users can still log in using email and password authentication.

Once you've properly configured Google Sign-In in the Firebase Console and updated the google-services.json file, you can revert the changes in LoginActivity.kt to enable Google Sign-In functionality again.