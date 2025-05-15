# Mutism
![sfff](https://github.com/user-attachments/assets/01f20f72-d369-4031-9390-be9e80cba31c)

> A mobile solution designed to protect the daily lives of individuals on the autism spectrum who experience difficulties due to sound sensitivity.  
> Through real-time sound detection, **the Mutism app helps reduce anxiety from triggering noises and enhances users’ independence and safety.**




## 🌍 UN SDGs Aligned
<img src="https://github.com/user-attachments/assets/c9850405-a80a-4cf7-91ec-af98818641fd" alt="sdg" style="width:30%;"/>




## 📑 Table of Contents

1. [Overview](#overview)
2. [User Research & Validation Process](#user-Research-&-Validation-Process)
3. [MVP](#mVP)
4. [Tech](#tech)
5. [Screen](#screen)
6. [Expected Effects](#expected-effects)
7. [Future Development](#future-Development)
9. [Member](#member)


## 🧩 Overview

### Autism?
![autism](https://github.com/user-attachments/assets/de1ef840-9c53-4521-b1aa-364e39ba427f)
Autism spectrum disorder (ASD) often includes heightened sensitivity to environmental sounds.  
Mutism aims to empower users with real-time detection and personalized responses to stressful auditory environments.

![KakaoTalk_Photo_2025-05-16-01-40-25 003](https://github.com/user-attachments/assets/d48847aa-16e7-4f02-b044-14be311f4da9)

Currently, about 1 in 100 people worldwide are diagnosed with autism, and among them, approximately 86.6% experience auditory hypersensitivity, making daily life challenging due to sound-related difficulties.

https://www.who.int/news-room/fact-sheets/detail/autism-spectrum-disorders

### Pain Point 
- **Severe Anxiety from Sudden Noises**
    - Unexpected sounds (e.g., honking, barking) can cause sensory overload, leading to panic.
    - Individuals may not know how to cope with the real-time noise, often displaying hypersensitive reactions and having difficulty calming down.
- **Limitations of Conventional Noise Blocking**
    - Even individuals capable of some self-regulation can only respond by wearing noise-canceling devices or covering their ears.
    - Since users must take manual action, it’s difficult to respond quickly when exposed to sudden sounds.
- **Lack of Immediate Intervention by Caregivers**
    - If a caregiver is not nearby, it becomes difficult to provide real-time support.
    - There is a need for an emergency function that can instantly notify and connect with a caregiver in crisis situations.

## 📋 User Research & Validation Process

### 📌 Background Research & Problem Definition

Individuals on the autism spectrum are often highly sensitive to sensory input, especially certain sounds, which can cause extreme discomfort. After conducting desk research, we wanted to understand the real-life challenges they face in daily life. To gain deeper insight, we conducted an **interview with a social worker at the Sungmin Welfare Center**.

- **Discomfort with Noise**
    - They are especially sensitive to sudden loud noises such as motorcycle horns in daily environments. We confirmed through the interview that individuals with autism do indeed experience significant discomfort from these kinds of sounds.
- **Current Coping Behaviors**
    - They often try to calm themselves by wearing noise-canceling headphones or covering their ears. According to the social worker, some individuals at the center who are particularly sensitive to sound even react by suddenly covering their ears very tightly.

Through this process, we found that many autistic individuals frequently engage in physical actions to block out disturbing sounds and that it’s often difficult for them to cope without the help of a caregiver.

> “There needs to be a way for autistic individuals to escape from overwhelming sounds without having to physically cover their ears.”

### 🛠️ Feature Specification & Iteration Through Feedback

Through a second interview with the welfare center, we refined the features of our solution focused on managing sensitivity to specific noises.

- Since each autistic individual is sensitive to different types of sounds, we designed a **personalized soothing message feature** tailored to each user’s noise preferences. The feedback we received was very positive, with the response that this feature could be highly beneficial in helping autistic individuals manage daily life independently without a caregiver.
- We discovered that **age or gender information is not particularly relevant** when setting up user profiles. Initially, we planned to collect this information to help the AI generate more personalized calming messages, but based on the feedback, we revised the user input fields accordingly.
- To prepare for situations where the user is exposed to distressing noises for extended periods, we added a feature where **repeated detection of sensitive sounds triggers an alert to the caregiver**, allowing them to check in on the user's status in real time.

As a result, we clearly defined the app’s direction as:

**Personalized noise classification → Delivery of soothing messages**,

and improved the feature set based on direct feedback.

<img src="https://github.com/user-attachments/assets/f51ecbba-d3ad-4839-ae41-2d7467778309" width="700" />

### 🧪 User Testing

After developing the app, we conducted interviews and user testing with **two individuals with severe autism and a social worker** at Seongmin Welfare Center to evaluate the app’s usability and determine whether it would genuinely benefit autistic individuals.

According to the social worker, although the participating individuals were classified as having severe autism in terms of behavior and intellectual level, they had relatively good cognitive abilities and were capable of self-regulation thanks to education received at the center. While they could understand instructions, they had limited expressive ability. Therefore, the **social worker's professional input played a crucial role** in helping us further improve the app.

<img src="https://github.com/user-attachments/assets/a0112105-c3e5-4945-a1e4-3f61ab9bb650" width="700" />

Since the app is in English, we first played a **screen recording with Korean subtitles** to help participants understand the overall flow.

At this point, regarding the **white noise selection feature**, the participants mentioned that it was well-designed because autistic individuals often prefer sounds like **forest ambience or bubbles**, and these preferences were well reflected in the app.
<img src="https://github.com/user-attachments/assets/62531084-8389-40bb-b248-8525aca3669c" width="700" />


Finally, we received feedback on the strengths of our app and suggestions for additional features.

First, participants expressed a strong preference for the color blue in the design. They also noted that the **voice conversion feature** helped increase comfort and a sense of calm for autistic users. They especially appreciated that the app was designed to provide **personalized calming messages** rather than generic, uniform ones. Overall, the autistic participants showed a desire to download and use the app, recognizing its meaningful solution to the problem of enduring discomfort caused by noise.

Additionally, the feedback pointed out that the **SOS button** might be difficult for users to operate directly, and that in cases of prolonged exposure to sensitive noise, automatic calls to caregivers might not allow for immediate assistance. However, the idea of informing caregivers about the user’s situation was seen as valuable. Based on this, we plan to improve this feature by sending **alert messages via SMS** in the future.

## 🚀 MVP
![Slide 16_9 - 23](https://github.com/user-attachments/assets/6a3264a9-5919-43e9-904f-fcba147d2f1e)

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




## 🛠️ Tech 

### Tech stack


#### Language

- **Kotlin**: Used for Android app development.

#### Architecture

- **MVC (Model - View - Controller)**
    - A simple structure with minimal asynchronous network response handling or complex state management.
    - Since the service primarily operates on-device with minimal external communication, the MVC pattern was considered more structurally efficient.

#### Data Management

- **EncryptedSharedPreferences**: Used to securely store user information.

#### AI

- **Gemini API (Gemini-2.0-flash)**
    - Suitable for generating personalized text considering various individual characteristics.
    - Capable of creative text generation, making it well-suited for TTS (Text-to-Speech) applications.
- **YAMNet**
    - A lightweight sound classification model for real-time environmental noise detection.
    - Optimized for mobile environments.
- **TensorFlow Lite**
    - A lightweight ML framework that enables efficient execution of models on mobile and IoT devices.
    - Used to run the YAMNet model quickly and efficiently on mobile devices.

#### Network

- **OkHttp**: Supports efficient HTTP network communication.

### System Workflow
<img width="510" alt="스크린샷 2025-05-16 오전 1 44 23" src="https://github.com/user-attachments/assets/0026b17d-b796-468c-b188-9bd3fd674a8a" />

### Architecture
<img width="800" alt="스크린샷 2025-05-16 오전 1 38 58" src="https://github.com/user-attachments/assets/77f24e3c-c236-4288-bdeb-e9e5c406be25" />


## 📱 Screen

### Home
<p float="left">
  <img src="https://github.com/user-attachments/assets/ff87fe5f-2464-41c1-bf50-8af97a016b31" width="170" />
  <img src="https://github.com/user-attachments/assets/60bb2309-53ab-4ad5-8e8c-193e52c3729f" width="170" />
</p>

- **When the Start button is pressed, sound classification begins in the background.**
- **The classified sound is displayed on the screen.**

### Emergency contact
<p float="left">
  <img src="https://github.com/user-attachments/assets/287b2767-16d6-4d49-8715-0a00dc935fbd" width="170" />
  <img src="https://github.com/user-attachments/assets/822fd73e-f7de-4673-9ac5-e98bfc56aa39" width="170" />
</p>

- **When the user presses the SOS button, the app immediately dials the emergency contact registered in their profile.**
- **If a sensitive sound is continuously detected for a certain period during real-time noise monitoring, the app automatically connects a call to the emergency contact.**

### Real-time Noise Detection / White noise playing
<p float="left">
  <img src="https://github.com/user-attachments/assets/5b525978-a2a8-4683-8bfb-afafa7534d14" width="170" />
  <img src="https://github.com/user-attachments/assets/c6415419-8078-46f2-9aec-a3b66c00f07f" width="170" />
</p>

- When real-time noise detection begins, tags representing the currently detected sound are displayed at the bottom of the screen.
- If a sensitive sound is detected, a calming voice message is played to help soothe the user (*refer to the PPT video*).
- After the calming message finishes, the white noise previously selected by the user is automatically played.
- While the white noise is playing, a ‘Stop White Noise’ button appears on the screen.
    - Every 5 minutes, a toast message and notification prompt the user to check their current state and encourage them to stop the white noise if they feel calm.

### Mypage / NoiseSelect / WhiteNoise
| <img src="https://github.com/user-attachments/assets/1af30430-1aed-457c-80ff-ba4c09c91a20" width="170"/> | <img src="https://github.com/user-attachments/assets/7032c3b7-01c3-4dac-9aa7-47859280df34" width="170"/> | <img src="https://github.com/user-attachments/assets/f51b1fbe-bfa1-4176-b2e3-0ba0ab8270a8" width="170"/> |
|:--:|:--:|:--:|
| Mypage | NoiseSelect | WhiteNoise |
- On the My Page screen, users can enter their personal information and access buttons that lead to the Noise Selection and White Noise Selection pages.
- In the Noise Selection page, sounds are categorized into main, sub, and detailed categories. Users can select tags using tabs and save their preferences.
- In the White Noise Selection page, users can choose and save one preferred white noise; only one option can be selected at a time.

### Enter User Information & Select Noises Dialog
| <img src="https://github.com/user-attachments/assets/ac37d575-3a08-4d39-b0f8-6fb27a843a0c" width="170"/> | <img src="https://github.com/user-attachments/assets/9a247745-dab9-47da-a99e-a5a966a58675" width="170"/> |
|:--:|:--:|
| User Information Input Dialog | Noise Selection Dialog |
- If the user attempts to start real-time noise detection without entering all required information, a modal appears guiding them to the My Page screen to complete their profile.
- Similarly, if the user hasn't selected any sensitive sounds, a modal prompts them to go to the My Page screen to complete sound selection before proceeding.

### Real-time Noise Detection Alert
<p float="left">
  <img src="https://github.com/user-attachments/assets/a555eb4f-3660-4aa1-8523-9bad8d54ef69" width="170" />
  <img src="https://github.com/user-attachments/assets/47c5dd0b-bf9c-4075-a6f5-9a18cf41379b" width="170" />
</p>


## 💡 Expected Effects
- **Supporting Safe Daily Life for Individuals on the Autism Spectrum**
    
    - Mutism classifies and detects ambient sounds in real time, alerting users when sensitive sounds are detected.
    
    - This helps users avoid overstimulation and enhances their sense of safety in everyday life.
    
- **Promoting Emotional Stability with Personalized Calming Messages and White Noise**
    
    - The app helps users who are sensitive to external stimuli regain emotional stability quickly and provides an environment to prevent sensory overload.
    
- **Enhancing Independence and Autonomy**
    
    - Users can manage their own calming process without relying on caregivers, promoting greater independence in daily routines.
    
- **Reducing Caregiver Burden with Emergency Alerts**
    
    - If a user is repeatedly exposed to a sensitive sound, the app automatically notifies the caregiver.
    
    - This allows for real-time monitoring and quick intervention, even when caregivers are not physically present.
    
- **Delivering Personalized Calming Strategies Based on User Information**
    
    - Using Gemini, Mutism provides personalized calming messages tailored to each user.
    
    - By analyzing both sound sensitivities and sound preferences, the app delivers truly customized support.

## 🏃‍♀️ Future Development
- **User-Specific On-Device AI**
    
    - We plan to enhance personalized calming solutions by allowing the on-device AI model to learn and adapt to each user's unique data.
    
- **Integration with Noise-Canceling Wearable Devices**
    
    - By connecting with wearable devices equipped with active noise canceling, we aim to provide fundamental sound-blocking functionality.
    
- **Caregiver Voice-Based Calming Messages**
    
    - Calming messages will be delivered using the caregiver’s voice to offer greater emotional comfort and a sense of security.
    
- **Personalized Safe Space Recommendations**
    
    - The app will suggest nearby safe and quiet places tailored to the user's preferences and needs.

## 💙 Member
|Member|[Eunsoo Kim](https://github.com/lyraa88)|[Jaewon Homg](https://github.com/jaewonderland)|[Sieun ko](https://github.com/withoutsummer)|[Hyojin Lim](https://github.com/hyojin425)|
| :--: | :--: | :--: | :--: | :--: |
| Role | AI | Design | Android | Android/Server |
| profile | <img src="https://github.com/user-attachments/assets/63b62f33-8039-40b0-818a-e73456e67129" width="100"/> | <img src="https://github.com/user-attachments/assets/dcf343b2-b55b-460a-914e-144a00330a0a" width="100"/> | <img src="https://github.com/user-attachments/assets/your-android-img-url" width="100"/> | <img src="https://github.com/user-attachments/assets/37312ae8-4991-4491-857e-d06b1d810891" width="100"/> |

![MUTISM](https://github.com/user-attachments/assets/2c5c46de-7d6e-4d11-975e-93414cda6bef)
