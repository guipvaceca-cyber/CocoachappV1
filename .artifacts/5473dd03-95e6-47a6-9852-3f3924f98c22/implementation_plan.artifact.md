# Implementation Plan - Fix Notes à chaud (Microphone & Auto-scroll)

This plan fixes the microphone not opening on some devices and ensures the "Notes à chaud" input field is always visible when the keyboard is open.

## User Review Required

> [!IMPORTANT]
> The microphone issue is likely due to the app trying to record audio and transcribe speech simultaneously. I will modify the system to allow "transcription only" mode, which is more reliable for field notes.

## Proposed Changes

### [Component] [Data] [LocalVoiceManager](file:///C:/Users/guip3/AndroidStudioProjects/CoachApp/app/src/main/java/com/example/coachapp/data/LocalVoiceManager.kt)

#### [MODIFY] [LocalVoiceManager.kt](file:///C:/Users/guip3/AndroidStudioProjects/CoachApp/app/src/main/java/com/example/coachapp/data/LocalVoiceManager.kt)
- Update `startListeningAndRecording(onlyTranscription: Boolean = false)`:
    - If `onlyTranscription` is true, skip `MediaRecorder` setup.
    - Only start `SpeechRecognizer`.

### [Component] [UI] [SessionCompanionScreen](file:///C:/Users/guip3/AndroidStudioProjects/CoachApp/app/src/main/java/com/example/coachapp/ui/screens/SessionCompanionScreen.kt)

#### [MODIFY] [SessionCompanionScreen.kt](file:///C:/Users/guip3/AndroidStudioProjects/CoachApp/app/src/main/java/com/example/coachapp/ui/screens/SessionCompanionScreen.kt)
- In `SessionCompanionScreen`:
    - Call `voiceManager.startListeningAndRecording(onlyTranscription = true)` when clicking the mic.
- In `LiveFeedbackInput`:
    - Add `onFocusChanged` to the `OutlinedTextField`.
    - Trigger an automatic scroll to the bottom of the `ScrollState` when the field gains focus.

## Verification Plan

### Manual Verification
- Deploy to a device.
- Navigate to "Terrain".
- Click the microphone: verify it starts (pulsing red) and transcribes speech without crashing or failing to open.
- Click the text field: verify the screen automatically scrolls down so the input field is visible above the keyboard.
