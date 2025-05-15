# Mutism
![sfff](https://github.com/user-attachments/assets/01f20f72-d369-4031-9390-be9e80cba31c)

> A mobile solution designed to protect the daily lives of individuals on the autism spectrum who experience difficulties due to sound sensitivity.  
> Through real-time sound detection, **the Mutism app helps reduce anxiety from triggering noises and enhances users’ independence and safety.**

---


## 🌍 UN SDGs Aligned
<img src="https://github.com/user-attachments/assets/c9850405-a80a-4cf7-91ec-af98818641fd" alt="sdg" style="width:30%;"/>


---

## 📑 Table of Contents

1. [Overview](#overview)
2. [Interviews & User Testing](#interviews--user-testing)
3. [MVP](#mvp)
4. [Tech Stack](#tech-stack)
5. [Screenshots](#screenshots)
6. [Expected Effects](#expected-effects)
7. [Future Plans](#future-plans)
8. [Team Members](#team-members)

---

## 🧩 Overview

### Autism?
![autism](https://github.com/user-attachments/assets/de1ef840-9c53-4521-b1aa-364e39ba427f)
> Autism spectrum disorder (ASD) often includes heightened sensitivity to environmental sounds.  
> Mutism aims to empower users with real-time detection and personalized responses to stressful auditory environments.

---

## 📋 Interviews & User Testing

_(Details about field interviews and UX testing here)_

---

## 🚀 MVP

### 1. **Real-Time Noise Detection and Classification**

Mutism uses the **YAMNet model** to classify environmental sounds collected in real-time, enabling accurate noise detection.

Before starting detection, users select the specific sounds they are sensitive to.

- The main screen displays real-time sound tags (e.g., *car*, *crying*, *shouting*), allowing users to intuitively understand their current auditory environment.
- The app leverages a foreground service, ensuring that even when the screen is off or the app is running in the background, it can still detect sensitive sounds and send **push notifications**. This enables users to use the app safely even when outside.



### 2. **Personalized Calming Messages + White Noise Playback**

When a sensitive sound is detected, a **personalized calming message** is generated using the **Gemini API**, based on the user’s profile information, and played aloud via TTS.

*(e.g., “You’re safe now. Let’s try finding a quieter place.”)*

- The calming message takes into account the user's name, age, and preferred relaxation methods.
- Once the voice message ends, the **user's pre-selected white noise** (e.g., ocean waves, birds chirping) automatically plays to help promote **emotional stability**.



### 3. **Automatic Guardian Notification for Prolonged Exposure**

If sensitive sounds are detected repeatedly over a certain threshold, the situation is regarded as **potentially dangerous**, and the app will **automatically call the guardian registered in the user’s profile**.

- The guardian’s phone number can be entered through the user profile, and in emergencies, the app initiates a call to notify someone nearby of the user’s condition.
- This feature serves as a **practical safety net** for users on the autism spectrum who are sensitive to sound.

### Key Features
![KakaoTalk_Photo_2025-05-16-01-40-26 006](https://github.com/user-attachments/assets/de6ed91c-01e8-48ec-8b6c-7b4345dcb75c)


---

## 🛠️ Tech Stack

| Component              | Technology                     |
|------------------------|---------------------------------|
| Frontend               | Android (Kotlin)               |
| Backend (AI)           | FastAPI, Whisper, Gemini API   |
| Sound Classification   | YAMNet (TFLite)                |
| Realtime TTS           | Android TextToSpeech API       |
| Storage & Auth         | Firebase                       |

### Language

- **Kotlin**: Used for Android app development.

### Architecture

- **MVC (Model - View - Controller)**
    - A simple structure with minimal asynchronous network response handling or complex state management.
    - Since the service primarily operates on-device with minimal external communication, the MVC pattern was considered more structurally efficient.

### Data Management

- **EncryptedSharedPreferences**: Used to securely store user information.

### AI

- **Gemini API (Gemini-2.0-flash)**
    - Suitable for generating personalized text considering various individual characteristics.
    - Capable of creative text generation, making it well-suited for TTS (Text-to-Speech) applications.
- **YAMNet**
    - A lightweight sound classification model for real-time environmental noise detection.
    - Optimized for mobile environments.
- **TensorFlow Lite**
    - A lightweight ML framework that enables efficient execution of models on mobile and IoT devices.
    - Used to run the YAMNet model quickly and efficiently on mobile devices.

### Network

- **OkHttp**: Supports efficient HTTP network communication.

---

## 📱 Screenshots

_(Insert multiple screen image examples here)_

---

## 💡 Expected Effects

- Reduce anxiety caused by environmental noise

