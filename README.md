# Indoor Navigation App

This Android application helps you build and test indoor navigation paths using Wi-Fi-based RSSI localization, path graphs, room and router placement, and visual testing. The app allows users to manually define traversable paths and associate rooms and routers to a floor plan image.

---

## 🔧 Features

* Upload a floor plan and define real-world dimensions.
* Define walkable grid zones.
* Draw path graphs by tapping to set start and end points.
* Place routers with SSIDs.
* Name and mark rooms.
* Export entire setup as a JSON file.
* Perform live RSSI-based localization and test routing.

---

## 🛠 Setup Instructions

1. Clone the repository:

   ```bash
   git clone https://github.com/yourusername/indoor-nav-app.git
   ```
2. Open the project in Android Studio.
3. Run the app on a physical Android device.

---

## 📱 App Usage Flow

Each screen is self-guided and expects user interactions to build the indoor map step-by-step:

### 1. Floor Plan Upload

* Upload an image of your floor.
* Enter the width and height of the real-world floor in meters.

### 2. Grid Drawing

* Tap on grid cells to mark walkable zones.
* Use zoom/pan for precise editing.
* Use Clear or Reset as needed.

### 3. Path Graph Editor

* **Tap once** to start a path.
* **Tap again** to end it.
* Nodes will auto-connect and snap to grid.
* Press "Test A\*" to validate routing.

### 4. Router Placement

* Tap on the image to add a router.
* Enter the router's SSID in the dialog.
* You can clear or edit as needed.

### 5. Room Naming

* Tap on a room location.
* Enter the room name.
* Room names will be saved to the setup.

### 6. Export Setup

* Save the map configuration.
* It includes image, paths, routers, rooms, and dimensions.

### 7. Execution Screen

* Load the saved setup.
* Scans live RSSI.
* Kalman filtering applied.
* Live user location is projected.

---

## 🧪 Testing Notes

* Minimum 3 routers needed for trilateration.
* Try walking around and check the live updates.
* Touch accuracy may vary slightly depending on scaling and density.

---

## 🗂 File Structure

* `PathGraphEditorScreen.kt` - Graph drawing logic.
* `RouterPlacementScreen.kt` - Add routers.
* `RoomNamingScreen.kt` - Add rooms.
* `ExecutionScreen.kt` - Test and visualize RSSI-based location.
* `ExportUtils.kt` - JSON export logic.

---

## 📜 License

MIT License.

---

## 🤝 Contribution

PRs and suggestions welcome!
