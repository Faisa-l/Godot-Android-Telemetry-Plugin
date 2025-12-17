This is my Advanced Technologies project repository. It uses a [custom plugin](https://github.com/Faisa-l/Godot-Android-Telemetry-Plugin) which fetches device sensor data.

It is a prototype of a mobile phone app, though there is a version for desktops.

# Features
- Schedule activites you want to perform.
- Automatically detects when activites should be done and (emulates when it would) tracks your performance of these activites.
- There is a walking activity which will track your steps (mobile only - will otherwise passively increment).
- There is a cycling activity which passively increments its activity results.
- The results from these activities update the visibility of a pet.
- And you can add friends and add them to activites (minus the network connectivity part).

# Requirements
For the mobile app:
- **Android 14 or higher** is required to run the app.

For the desktop app:
- Windows only (10/11).
- There might be a Linux version.

# Extra notes

For the app to fully run on mobile devices, you will need to accept activity tracking and push notification systems. 
**A part of the app is dedicated to passively collecting step counter sensor data, and it needs permissions for this to work**. 
The desktop version does not have this functionality.
