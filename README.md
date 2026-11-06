<div align="center">
<img src="https://i.postimg.cc/MHr8wWPr/Truck-Orbit.png" alt="" />
</div>

# Truck Orbit 🚚🌍

Truck Orbit is an app designed for truck owners with multiple vehicles. With real-time GPS tracking, owners can easily monitor their trucks' locations, manage driver accounts, and ensure efficient fleet management.

### Features:
- **Real-Time GPS Location Tracking** 📍: Track your trucks' locations in real-time using Google Maps API.
- **Driver Management** 👨‍✈️👩‍✈️: Manage drivers, assign roles (Admin or Driver), and monitor truck statuses.

### Technologies Used:
- **Firebase** 🔥: User authentication and Firestore database.
- **Kotlin** 💻: The main programming language for Android development.
- **XML** 🧱: For creating the app's user interface.
- **Google Maps API** 🗺️: For real-time vehicle tracking.

### Setup Instructions:

#### Step 1: Set Up Firebase ⚡
1. Create a Firebase project in the Firebase console: [Firebase Console](https://console.firebase.google.com/).
2. Add Firebase to your Android project following the official guide: [Add Firebase to Your Android Project](https://firebase.google.com/docs/android/setup).
3. In the Firebase console, enable **Email Authentication** and **Firestore**.

#### Step 2: Add Google Maps API Key 🗺️
1. Obtain your API key from the Google Cloud Console: [Google Cloud Console](https://console.cloud.google.com/).
2. Add your API key in `AndroidManifest.xml`:
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="YOUR_MAPS_API_KEY" />
   ```

#### Step 3: Update Constants 📧
1. Open `data/Constants.kt` and update the `DEV_MAIL_ADDRESS` field to your own email address where all bug reports will be sent:
   ```kotlin
   const val DEV_MAIL_ADDRESS = "your-email@example.com"
   ```

#### Step 4: Set Admin Account 👑
1. To make an account an **Admin** for the first time, go to Firestore > `users` > find the user > edit the `accountType` field and change it from `DRIVER` to `ADMIN`.
2. Once the user is set as an **Admin**, they can manage user roles (Admin/Driver) from within the app.

---

### Note:
- Drivers share their real-time location, and truck owners can view it on the map.
- Admins can manage accounts and change roles directly from the driver list in the app.

Happy fleet management with Truck Orbit! 🚚💨
