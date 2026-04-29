import { create } from "zustand";
import { devtools, persist } from "zustand/middleware";



interface UIState {
  theme: "dark" | "light";
  toggleTheme: () => void;
  setTheme: (theme: "dark" | "light") => void;
  isRedirecting: boolean;
  setRedirecting: (v: boolean) => void;
}

export const useUIStore = create<UIState>()(
  devtools(
    persist(
      (set) => ({
        theme: "dark",
        toggleTheme: () => set((s) => ({ theme: s.theme === "dark" ? "light" : "dark" })),
        setTheme: (theme) => set({ theme }),
        isRedirecting: false,
        setRedirecting: (v) => set({ isRedirecting: v }),
      }),
      { name: "payflow-ui", partialize: (s) => ({ theme: s.theme }) }
    )
  )
);