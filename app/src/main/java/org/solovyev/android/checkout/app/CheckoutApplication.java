package org.solovyev.android.checkout.app;

import org.solovyev.android.checkout.Billing;

import android.app.Application;

import javax.annotation.Nonnull;

public class CheckoutApplication extends Application {

    @Nonnull
    private Billing mBilling = new Billing(this, new Billing.DefaultConfiguration() {
        @Nonnull
        @Override
        public String getPublicKey() {
            // encrypted public key of the app. Plain version can be found in Google Play's Developer
            // Console in Service & APIs section under "YOUR LICENSE KEY FOR THIS APPLICATION" title.
            // A naive encryption algorithm is used to "protect" the key. See more about key protection
            // here: https://developer.android.com/google/play/billing/billing_best_practices.html#key
            final String s = "PixnMSYGLjg7Ah0xDwYILlVZUy0sIiBoMi4jLDcoXTcNLiQjKgtlIC48NiRcHxwKHEcYEyZrPyMWXFRpV10VES9ENz" +
                    "g1Hj06HTV1MCAHJlpgEDcmOxFDEkA8OiQRKjEQDxhRWVVEMBYmNl1AJghcKUAYVT15KSQgBQABMgwqKSlqF1gZBA4fAw5rMyxKI" +
                    "w9LJFc7AhxZGjoPATgRUiUjKSsOWyRKDi4nIA9lKgAGOhMLDF06CwoKGFR6Wj0hGwReS10NXzQTIREhKlkuMz4XDTwUQjRCJUA+" +
                    "VjQVPUIoPicOLQJCLxs8RjZnJxY1OQNLKgQCPj83AyBEFSAJEk5UClYjGxVLNBU3FS4DCztENQMuOk5rFVclKz88AAApPgADGFx" +
                    "EEV5eQAF7QBhdQEE+Bzc5MygCAwlEFzclKRB7FB0uFgwPKgAvLCk2OyFiKxkgIy8BBQYjFy4/E1ktJikrEVlKJVYIHh16NDwtDC" +
                    "U0Vg8JNzoQBwQWOwk1GzZ4FT8fWicwITcRJi8=";
            return Encryption.decrypt(s, "se.solovyev@gmail.com");
        }
    });

    @Nonnull
    public Billing getBilling() {
        return mBilling;
    }
}
