package com.example.jake.sunshine.util;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;

import java.util.HashMap;
import java.util.Locale;

/**
 * Created by jake on 2/16/15.
 */
public class Speaker implements TextToSpeech.OnInitListener {
    
    private TextToSpeech tts;
    private boolean ready = false;
    private boolean allowed = false;
    
    public Speaker(Context context) {
        tts = new TextToSpeech(context, this);        
    }
    
    public boolean isAllowed() {
        return allowed;        
    }
    
    public void allow(boolean allowed) {
        this.allowed = allowed;
    }
    
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // Change this to match your locale
            tts.setLanguage(Locale.US);
            ready = true;
        } else {
            ready = false;
        }
    }
    
    public void speak(String text) {
        // Speak only if the TTS is ready and
        // the user has allowed speech_to_text
        if (ready && allowed) {
            Bundle bundle = new Bundle();
            bundle.putString("key_param_stream", TextToSpeech.Engine.KEY_PARAM_STREAM);
            bundle.putString("stream_notification", String.valueOf(AudioManager.STREAM_NOTIFICATION));
            // TODO: I don't know if I created this Bundle correctly.
            // TODO: Nor do I have any clue if I created the utteranceId correctly.
            tts.speak(text, TextToSpeech.QUEUE_ADD, bundle, "1");
        }
    }
    
    public void pause(int duration) {
        tts.playSilentUtterance(duration, TextToSpeech.QUEUE_ADD, null);
    }

    /**
     * Free up resources 
     */
    public void destroy() {
        tts.shutdown();
    }
}
