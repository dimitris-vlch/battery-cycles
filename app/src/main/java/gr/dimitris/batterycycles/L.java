package gr.dimitris.batterycycles;

import java.util.Locale;

/** Tiny two-language helper: Greek when the phone is in Greek, English otherwise. */
final class L {
  static String s(String el, String en) {
    return "el".equals(Locale.getDefault().getLanguage()) ? el : en;
  }
}
