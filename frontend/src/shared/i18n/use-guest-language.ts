"use client";

import { useSyncExternalStore } from "react";
import {
  DEFAULT_UI_LANGUAGE,
  resolveUiLanguage,
  type UiLanguage,
} from "./language";

const storageKey = "visafe.uiLanguage";
const changeEvent = "visafe:ui-language-change";
let temporaryLanguage: UiLanguage | undefined;

function subscribe(onChange: () => void) {
  function onStorage(event: StorageEvent) {
    if (event.key === storageKey || event.key === null) {
      onChange();
    }
  }

  window.addEventListener(changeEvent, onChange);
  window.addEventListener("storage", onStorage);
  return () => {
    window.removeEventListener(changeEvent, onChange);
    window.removeEventListener("storage", onStorage);
  };
}

function getGuestLanguage(): UiLanguage {
  if (temporaryLanguage !== undefined) {
    return temporaryLanguage;
  }

  try {
    return resolveUiLanguage(window.localStorage.getItem(storageKey));
  } catch {
    return DEFAULT_UI_LANGUAGE;
  }
}

function setGuestLanguage(language: UiLanguage) {
  try {
    window.localStorage.setItem(storageKey, language);
    temporaryLanguage = undefined;
  } catch {
    temporaryLanguage = language;
  }

  window.dispatchEvent(new Event(changeEvent));
}

export function useGuestLanguage() {
  const language = useSyncExternalStore(
    subscribe,
    getGuestLanguage,
    () => DEFAULT_UI_LANGUAGE,
  );

  return { language, setGuestLanguage };
}
