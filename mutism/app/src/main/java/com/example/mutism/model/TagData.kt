package com.example.mutism.model

val tagTabTitles =
    listOf(
        "Human Sounds",
        "Music & Art",
        "Electronic Devices",
        "Machines & Tools",
        "Transportation",
        "Nature",
        "Impact & Danger",
        "Everyday Objects",
        "Crowd & Environment",
        "Others",
    )

val tagContents: Map<String, List<TagSection>> =
    mapOf(
        "Human Sounds" to
            listOf(
                TagSection("Speech", listOf("Speech", "Whispering", "Shout", "Narration", "Conversation")),
                TagSection("Emotional Expression", listOf("Laughter", "Crying", "Screaming", "Sigh", "Groan", "Moan")),
                TagSection("Infant / Child", listOf("Baby cry", "Babbling", "Child speech")),
                TagSection("Breathing", listOf("Breathing", "Wheeze", "Snore", "Gasp")),
                TagSection("Other Body Sounds", listOf("Cough", "Sneeze", "Hiccup", "Burping", "Sniff")),
            ),
        "Music & Art" to
            listOf(
                TagSection("Vocal", listOf("Singing", "Chant")),
                TagSection("Instruments", listOf("Guitar", "Piano", "Violin", "Flute", "Trumpet", "Drum", "Cymbal", "Tambourine")),
                TagSection("Electronic / Rhythm", listOf("Synthesizer", "Beatboxing", "Electronic music")),
            ),
        "Electronic Devices" to
            listOf(
                TagSection("Alerts & Alarms", listOf("Alarm", "Beep", "Buzzer", "Siren", "Ringtone", "DTMF")),
                TagSection("Communication", listOf("Telephone bell", "Message alert", "Notification")),
                TagSection("Device Operation", listOf("Microwave", "Washing machine", "Printer", "Camera shutter")),
            ),
        "Machines & Tools" to
            listOf(
                TagSection("Household Tools", listOf("Blender", "Drill", "Hair dryer", "Vacuum cleaner", "Electric toothbrush")),
                TagSection("Engines & Motors", listOf("Engine starting", "Idling", "Accelerating", "Electric fan")),
                TagSection("Industrial Machines", listOf("Power tool", "Chainsaw", "Lawn mower")),
            ),
        "Transportation" to
            listOf(
                TagSection("Road Traffic", listOf("Car", "Motorcycle", "Truck", "Bus")),
                TagSection("Rail / Air", listOf("Train", "Subway", "Airplane", "Helicopter")),
            ),
        "Nature" to
            listOf(
                TagSection("Weather", listOf("Rain", "Thunderstorm", "Wind", "Snow")),
                TagSection("Natural Environment", listOf("Ocean", "Stream", "River", "Fire")),
                TagSection("Animal Sounds", listOf("Dog bark", "Cat meow", "Birdsong", "Insect buzz", "Frog croak")),
            ),
        "Impact & Danger" to
            listOf(
                TagSection("Explosion / Gunfire", listOf("Explosion", "Gunshot", "Fireworks")),
                TagSection("Breakage / Impact", listOf("Glass breaking", "Bang", "Slap", "Crash", "Smash")),
            ),
        "Everyday Objects" to
            listOf(
                TagSection("Doors / Furniture", listOf("Door knock", "Door creak", "Drawer open", "Zipper")),
                TagSection("Small Contact", listOf("Tap", "Knock", "Coin drop", "Object rustle", "Clapping")),
            ),
        "Crowd & Environment" to
            listOf(
                TagSection("Crowd Ambience", listOf("Crowd", "Applause", "Cheering", "Footsteps", "Chatter")),
                TagSection("Noise / Background", listOf("Static", "White noise", "Background noise", "Echo", "Hum")),
            ),
        "Others" to
            listOf(
                TagSection("Unclassified / Silent", listOf("Silence", "Unknown", "Heart sounds", "ASMR")),
            ),
    )
