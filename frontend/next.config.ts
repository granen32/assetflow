import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  async rewrites() {
    // 브라우저는 같은 출처의 /api를 호출하고 Next 개발 서버가 BE로 전달한다.
    // 로컬 개발용 프록시이므로 공개 배포 시 별도 API 게이트웨이/인증을 설계해야 한다.
    return [{ source: "/api/:path*", destination: `${process.env.API_ORIGIN ?? "http://localhost:8080"}/api/:path*` }];
  },
};

export default nextConfig;
