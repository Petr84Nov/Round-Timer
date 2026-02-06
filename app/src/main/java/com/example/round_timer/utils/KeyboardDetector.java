package com.example.round_timer.utils;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.WindowInsetsAnimation;

import java.util.List;

/**
 * Třída pro detekci zobrazení a skrytí klávesnice na Android zařízení.
 *
 * Použití:
 * ```
 * KeyboardDetector keyboardDetector = new KeyboardDetector(activity);
 * keyboardDetector.setKeyboardListener(new KeyboardDetector.KeyboardListener() {
 *     @Override
 *     public void onKeyboardOpen() {
 *         // Kód vykonaný při otevření klávesnice
 *     }
 *
 *     @Override
 *     public void onKeyboardClose() {
 *         // Kód vykonaný při zavření klávesnice
 *     }
 * });
 *
 * // Po dokončení nezapomeňte zavolat
 * keyboardDetector.release();
 * ```
 */

public class KeyboardDetector {

    private static final String TAG = "KeyboardDetector";
    private final Activity activity;
    private KeyboardListener keyboardListener;
    private boolean isKeyboardVisible = false;
    private ViewTreeObserver.OnGlobalLayoutListener globalLayoutListener;
    private View rootView;
    private WindowInsetsAnimation.Callback insetsAnimationCallback;
    private View decorView;

    /**
     * Rozhraní pro callback události klávesnice s informací o View s fokusem
     */
    public interface KeyboardListener {
        void onKeyboardOpen(View focusedView);
        void onKeyboardClose(View focusedView);
    }

    public KeyboardDetector(Activity activity) {
        this.activity = activity;
        setupKeyboardDetection();
    }

    public void setKeyboardListener(KeyboardListener listener) {
        this.keyboardListener = listener;
    }

    private void setupKeyboardDetection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Použít moderní WindowInsets API pro Android 11+
            setupWindowInsetsAnimationCallback();
        } else {
            // Fallback pro starší verze Androidu
            setupLegacyDetection();
        }
    }

    private void setupWindowInsetsAnimationCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            decorView = activity.getWindow().getDecorView();

            insetsAnimationCallback = new WindowInsetsAnimation.Callback(
                    WindowInsetsAnimation.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE) {

                @Override
                public void onPrepare(WindowInsetsAnimation animation) {
                    // Příprava na animaci
                }

                @Override
                public WindowInsetsAnimation.Bounds onStart(WindowInsetsAnimation animation,
                                                            WindowInsetsAnimation.Bounds bounds) {
                    return bounds;
                }

                @Override
                public WindowInsets onProgress(WindowInsets insets,
                                               List<WindowInsetsAnimation> runningAnimations) {

                    if (!runningAnimations.isEmpty()) {
                        // Kontrola, zda animace zahrnuje klávesnici
                        for (WindowInsetsAnimation animation : runningAnimations) {
                            if ((animation.getTypeMask() & WindowInsets.Type.ime()) != 0) {
                                // Zjistit, zda je klávesnice viditelná
                                boolean keyboardVisible = insets.isVisible(WindowInsets.Type.ime());

                                if (keyboardVisible != isKeyboardVisible) {
                                    isKeyboardVisible = keyboardVisible;
                                    //notifyKeyboardVisibilityChanged();
                                }
                                break;
                            }
                        }
                    }

                    return insets;
                }

                @Override
                public void onEnd(WindowInsetsAnimation animation) {
                    // Animace skončila
                }
            };

            // Přidání callback na dekorView
            decorView.setWindowInsetsAnimationCallback(insetsAnimationCallback);

            // Počáteční kontrola stavu klávesnice
            decorView.setOnApplyWindowInsetsListener((v, insets) -> {
                boolean keyboardVisible = insets.isVisible(WindowInsets.Type.ime());
                if (keyboardVisible != isKeyboardVisible) {
                    isKeyboardVisible = keyboardVisible;
                    //notifyKeyboardVisibilityChanged();
                }
                return v.onApplyWindowInsets(insets);
            });

            // Vyžádat WindowInsets
            decorView.requestApplyInsets();
        }
    }

    private void setupLegacyDetection() {
        rootView = activity.findViewById(android.R.id.content);

        globalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            private final Rect r = new Rect();
            private int lastVisibleDecorViewHeight = 0;

            @Override
            public void onGlobalLayout() {
                try {
                    // Zjištění výšky viditelné oblasti
                    rootView.getWindowVisibleDisplayFrame(r);

                    final int visibleDecorViewHeight = r.height();

                    // Pokud se výška nezměnila, nic se nestalo
                    if (lastVisibleDecorViewHeight == visibleDecorViewHeight) {
                        return;
                    }

                    lastVisibleDecorViewHeight = visibleDecorViewHeight;

                    final int rootViewHeight = rootView.getRootView().getHeight();
                    final int heightDiff = rootViewHeight - visibleDecorViewHeight;

                    // Když rozdíl je větší než 25% výšky rootView, předpokládáme, že klávesnice je otevřená
                    boolean isVisible = heightDiff > rootViewHeight * 0.25;

                    // Oznámit změnu stavu
                    if (isVisible != isKeyboardVisible) {
                        isKeyboardVisible = isVisible;
                        //notifyKeyboardVisibilityChanged();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Chyba při detekci klávesnice", e);
                }
            }
        };

        rootView.getViewTreeObserver().addOnGlobalLayoutListener(globalLayoutListener);
    }

    private void notifyKeyboardVisibilityChanged() {
        if (keyboardListener != null) {
            // Získáme aktuální view s fokusem v momentě události
            View focusedView = activity.getCurrentFocus();

            if (isKeyboardVisible) {
                keyboardListener.onKeyboardOpen(focusedView);
                Log.d(TAG, "Klávesnice otevřena, fokus na: " +
                        (focusedView != null ? focusedView.getClass().getSimpleName() : "žádný"));
            } else {
                keyboardListener.onKeyboardClose(focusedView);
                Log.d(TAG, "Klávesnice zavřena, fokus na: " +
                        (focusedView != null ? focusedView.getClass().getSimpleName() : "žádný"));
            }
        }
    }

    /**
     * Vrátí informaci o viditelnosti klávesnice
     */
    public boolean isKeyboardVisible() {
        return isKeyboardVisible;
    }

    /**
     * Vrátí aktuální view s fokusem
     */
    public View getCurrentFocusedView() {
        return activity.getCurrentFocus();
    }

    /**
     * Uvolní všechny prostředky a odstraní listenery
     */
    public void release() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (decorView != null) {
                decorView.setWindowInsetsAnimationCallback(null);
                decorView.setOnApplyWindowInsetsListener(null);
            }
        } else {
            if (rootView != null && globalLayoutListener != null) {
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(globalLayoutListener);
                rootView = null;
                globalLayoutListener = null;
            }
        }
        keyboardListener = null;
    }
}
