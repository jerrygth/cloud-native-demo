import { create } from "zustand";
import { devtools, persist } from "zustand/middleware";
export const useUIStore = create()(devtools(persist((set) => ({
    theme: "dark",
    toggleTheme: () => set((s) => ({ theme: s.theme === "dark" ? "light" : "dark" })),
    isRedirecting: false,
    setRedirecting: (v) => set({ isRedirecting: v }),
}), { name: "payflow-ui", partialize: (s) => ({ theme: s.theme }) })));
