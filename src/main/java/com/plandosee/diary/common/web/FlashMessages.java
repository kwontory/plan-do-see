package com.plandosee.diary.common.web;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * PRG result notice. Only a message key and optional arguments cross the redirect;
 * the template resolves the text from messages.properties.
 */
public final class FlashMessages {

    public static final String KEY = "flashKey";
    public static final String ARGS = "flashArgs";

    private FlashMessages() {
    }

    public static void add(RedirectAttributes redirect, String messageKey, Object... args) {
        redirect.addFlashAttribute(KEY, messageKey);
        if (args != null && args.length > 0) {
            redirect.addFlashAttribute(ARGS, args.clone());
        }
    }
}
