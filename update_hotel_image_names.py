import firebase_admin
from firebase_admin import credentials, db

# Path to your Firebase service account key
cred = credentials.Certificate('serviceAccountKey.json')
firebase_admin.initialize_app(cred, {
    'databaseURL': 'https://travelplannerapp-5617e-default-rtdb.firebaseio.com/'
})

# Mapping hotel names to drawable image names
hotel_name_to_image = {
    "PC Bhurban": "pc_bhurban",
    "Fairy Meadows Resort": "fairy_meadows_resort",
    "Pearl Continental Lahore": "pearl_continental_lahore",
    "Ratti Gali Lake Campsite": "ratti_gali_lake_campsite",
    # Add more mappings as needed
}

trips_ref = db.reference('trips')
trips = trips_ref.get()

for trip_id, trip_data in trips.items():
    selected_hotels = trip_data.get('selectedHotels', {})
    updated = False
    for idx, hotel in selected_hotels.items():
        name = hotel.get('name', '')
        image_name = hotel_name_to_image.get(name, name.lower().replace(' ', '_'))
        if image_name:
            hotel['imageName'] = image_name
            updated = True
            print(f"Set imageName for {name} in trip {trip_id} to {image_name}")
        else:
            print(f"WARNING: No imageName mapping for hotel '{name}' in trip {trip_id}")
    if updated:
        trips_ref.child(trip_id).child('selectedHotels').set(selected_hotels)

print("Done updating imageName fields.") 