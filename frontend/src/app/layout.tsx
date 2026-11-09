import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "AssetFlow — 나의 자산 관리",
  description: "자산과 현금흐름을 한눈에 관리하는 개인 금융 대시보드",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="ko"><body>{children}</body></html>;
}
