"use client";

import { FormEvent, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { ArrowDownRight, ArrowUpRight, BarChart3, ChevronDown, CircleHelp, CreditCard, LayoutDashboard, Plus, RefreshCw, ShieldCheck, Wallet, X } from "lucide-react";
import { Account, AccountType, ApiError, Member, assetApi } from "@/lib/api";

const money = (value: number) => new Intl.NumberFormat("ko-KR", { style: "currency", currency: "KRW", maximumFractionDigits: 0 }).format(value);
const accountLabels: Record<AccountType, string> = { CHECKING: "입출금", SAVINGS: "저축", CASH: "현금" };
const accountColors = ["purple", "orange", "blue", "green"];
export default function Home() {
  const [member, setMember] = useState<Member | null>(null);
  const memberId = member?.id ?? null;
  const nickname = member?.nickname ?? "";
  const sessionVersion = useRef(0);
  const [sessionLoading, setSessionLoading] = useState(true);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [modal, setModal] = useState<"login" | "register" | "account" | null>(null);
  const [email, setEmail] = useState("");
  const [memberName, setMemberName] = useState("");
  const [password, setPassword] = useState("");
  const [accountName, setAccountName] = useState("");
  const [accountType, setAccountType] = useState<AccountType>("CHECKING");
  const [balance, setBalance] = useState("");

  const loadAccounts = useCallback(async () => {
    const version = sessionVersion.current;
    setLoading(true);
    setError("");
    try {
      const result = await assetApi.listAccounts();
      if (version === sessionVersion.current) setAccounts(result);
    }
    catch (cause) { if (version === sessionVersion.current) setError(cause instanceof Error ? cause.message : "계좌 조회에 실패했습니다."); }
    finally { if (version === sessionVersion.current) setLoading(false); }
  }, []);

  useEffect(() => {
    let active = true;
    const version = sessionVersion.current;
    // 로컬 ID 대신 HttpOnly 쿠키로 현재 회원과 계좌를 복원한다.
    void assetApi.me().then(async current => {
      if (!active || version !== sessionVersion.current) return;
      setMember(current);
      const result = await assetApi.listAccounts();
      if (active && version === sessionVersion.current) setAccounts(result);
    }).catch(cause => {
      if (active && version === sessionVersion.current && !(cause instanceof ApiError && cause.status === 401)) {
        setError(cause instanceof Error ? cause.message : "로그인 상태를 확인하지 못했습니다.");
      }
    }).finally(() => { if (active) { setSessionLoading(false); setLoading(false); } });
    const expire = () => {
      sessionVersion.current += 1;
      setMember(null); setAccounts([]); setModal("login"); setPassword(""); setLoading(false);
      setError("로그인이 만료되었습니다. 다시 로그인해 주세요.");
    };
    window.addEventListener("assetflow:unauthorized", expire);
    return () => { active = false; window.removeEventListener("assetflow:unauthorized", expire); };
  }, []);

  const total = useMemo(() => accounts.reduce((sum, account) => sum + Number(account.balance), 0), [accounts]);
  const savings = useMemo(() => accounts.filter(a => a.type === "SAVINGS").reduce((sum, a) => sum + Number(a.balance), 0), [accounts]);

  async function submitMember(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setSaving(true); setError("");
    try {
      if (new TextEncoder().encode(password).length > 72) {
        setError("비밀번호는 UTF-8 기준 72바이트 이하여야 합니다."); return;
      }
      const member = modal === "register"
        ? await assetApi.register(email.trim(), memberName.trim(), password)
        : await assetApi.login(email.trim(), password);
      sessionVersion.current += 1;
      setMember(member);
      setAccounts([]); setLoading(true);
      setModal(null);
      setEmail(""); setMemberName(""); setPassword("");
      await loadAccounts();
    } catch (cause) { setError(cause instanceof Error ? cause.message : "로그인에 실패했습니다."); }
    finally { setSaving(false); }
  }

  async function logout() {
    setSaving(true); setError("");
    try {
      await assetApi.logout();
      sessionVersion.current += 1;
      setMember(null); setAccounts([]); setPassword(""); setModal(null); setLoading(false);
    } catch (cause) { setError(cause instanceof Error ? cause.message : "로그아웃에 실패했습니다."); }
    finally { setSaving(false); }
  }

  async function submitAccount(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (memberId === null) return;
    const amount = Number(balance);
    if (!Number.isFinite(amount) || amount < 0) { setError("0 이상의 금액을 입력해 주세요."); return; }
    setSaving(true); setError("");
    try {
      await assetApi.createAccount(accountName.trim(), accountType, amount);
      await loadAccounts(); setModal(null);
      setAccountName(""); setBalance("");
    } catch (cause) { setError(cause instanceof Error ? cause.message : "계좌 생성에 실패했습니다."); }
    finally { setSaving(false); }
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand"><span className="brand-icon"><BarChart3 size={22} strokeWidth={2.6} /></span><span>Asset<span className="brand-accent">Flow</span></span></div>
        <div className="nav-caption">WORKSPACE</div>
        <nav className="nav-list" aria-label="주 메뉴">
          <a className="nav-item active" href="#dashboard"><LayoutDashboard size={19}/> 대시보드</a>
          <a className="nav-item" href="#accounts"><Wallet size={19}/> 내 계좌</a>
          <span className="nav-item disabled"><ArrowUpRight size={19}/> 거래 내역 <span className="soon">예정</span></span>
          <span className="nav-item disabled"><BarChart3 size={19}/> 자산 분석 <span className="soon">예정</span></span>
        </nav>
        <div className="sidebar-bottom"><div className="learning-badge"><ShieldCheck size={18}/><div><b>Learning Build</b><small>Java / Spring Boot + Harness</small></div></div><p>AssetFlow v0.1.0</p></div>
      </aside>

      <div className="main-area" id="dashboard">
        <header className="topbar"><span className="crumb">내 워크스페이스 <span>/</span> <b>대시보드</b></span><div className="top-actions"><span className="env-pill"><span className="dot"/> LOCAL DEVELOPMENT</span>{member ? <><span className="profile" aria-label={`${nickname}님`}>{nickname.slice(0, 1)}</span><button className="outline-button" disabled={saving} onClick={() => void logout()}>로그아웃</button></> : <button className="outline-button" disabled={sessionLoading} onClick={() => { setError(""); setModal("login"); }}>로그인</button>}</div></header>
        <main className="content">
          <div className="page-heading"><div><div className="eyebrow">FINANCIAL OVERVIEW</div><h1>{nickname ? `${nickname}님의 자산 현황` : "내 자산, 한눈에 관리하세요"}</h1><p>자산의 흐름을 명확하게 파악하고 더 나은 재무 습관을 만들어 보세요.</p></div><button className="primary-button" disabled={sessionLoading} onClick={() => { setError(""); setModal(memberId ? "account" : "register"); }}><Plus size={17}/>{memberId ? "새 계좌 추가" : "시작하기"}</button></div>

          <div className="demo-banner"><CircleHelp size={18}/><span><b>로컬 학습 환경</b> — 로그인한 회원의 계좌만 표시합니다. 잔액은 현재 초기 잔액 기준입니다.</span></div>
          {error && <div className="error-banner" role="alert">{error}<button onClick={() => setError("")} aria-label="오류 닫기"><X size={16}/></button></div>}

          <section className="summary-grid" aria-label="자산 요약">
            <div className="stat-card main-stat"><div className="stat-head"><span>총 보유 자산</span><span className="stat-icon purple"><Wallet size={20}/></span></div><strong>{money(total)}</strong><div className="stat-foot"><span className="stat-tag">현재 기준</span> 등록된 계좌 잔액 합계</div></div>
            <div className="stat-card"><div className="stat-head"><span>저축 계좌 잔액</span><span className="stat-icon orange"><ArrowUpRight size={20}/></span></div><strong>{money(savings)}</strong><div className="stat-foot">저축 계좌에 등록된 자산</div></div>
            <div className="stat-card"><div className="stat-head"><span>등록된 계좌</span><span className="stat-icon blue"><CreditCard size={20}/></span></div><strong>{accounts.length}<span className="unit">개</span></strong><div className="stat-foot">입출금 · 저축 · 현금</div></div>
          </section>

          <div className="section-grid">
            <section className="panel accounts-panel" id="accounts"><div className="panel-heading"><div><h2>내 계좌</h2><p>연결된 자산을 한곳에서 관리하세요</p></div><button className="icon-button" title="새로고침" disabled={memberId === null || loading} onClick={() => memberId !== null && void loadAccounts()}><RefreshCw size={17} className={loading ? "spin" : ""}/></button></div>
              {sessionLoading ? <div className="empty-state"><RefreshCw className="spin" size={23}/><p>로그인 상태를 확인하는 중...</p></div>
                : memberId === null ? <div className="empty-state"><div className="empty-icon"><Wallet size={25}/></div><h3>로그인하고 자산을 관리하세요</h3><p>회원가입 후 나만의 계좌를 등록하고<br/>자산 현황을 확인할 수 있어요.</p><button className="outline-button" onClick={() => { setError(""); setModal("login"); }}>로그인</button></div>
                : loading ? <div className="empty-state"><RefreshCw className="spin" size={23}/><p>계좌 정보를 불러오는 중...</p></div>
                : accounts.length === 0 ? <div className="empty-state"><div className="empty-icon"><CreditCard size={25}/></div><h3>아직 등록된 계좌가 없어요</h3><p>첫 번째 계좌를 추가해 보세요.</p><button className="outline-button" onClick={() => setModal("account")}>계좌 추가하기</button></div>
                : <div className="account-list">{accounts.map((account, index) => <div key={account.id} className="account-row"><div className={`account-avatar ${accountColors[index % accountColors.length]}`}><Wallet size={20}/></div><div className="account-info"><strong>{account.name}</strong><span>{accountLabels[account.type]} · {account.currency}</span></div><strong className="account-amount">{money(Number(account.balance))}</strong></div>)}</div>}
              <div className="panel-footer"><span>{accounts.length}개의 계좌</span><button disabled={memberId === null} onClick={() => setModal("account")}>계좌 추가 <Plus size={15}/></button></div>
            </section>

            <section className="panel next-panel"><div className="panel-heading"><div><h2>개발 로드맵</h2><p>이번 프로젝트의 단계별 구현 현황</p></div><span className="roadmap-label">PHASE 01</span></div><div className="roadmap-list"><div className="roadmap-item done"><span className="roadmap-marker">✓</span><div><strong>기본 프로젝트 구조</strong><p>FE / BE / DB / Harness</p></div><span className="roadmap-status">구현</span></div><div className="roadmap-item done"><span className="roadmap-marker">✓</span><div><strong>회원 및 계좌 API</strong><p>Spring Data JPA + REST API</p></div><span className="roadmap-status">구현</span></div><div className="roadmap-item current"><span className="roadmap-marker">3</span><div><strong>거래 내역 관리</strong><p>입출금, 이체, 잔액 계산</p></div><span className="roadmap-status">다음 단계</span></div><div className="roadmap-item"><span className="roadmap-marker">4</span><div><strong>보안과 인증</strong><p>Spring Security + JWT</p></div></div></div><div className="roadmap-note"><ArrowDownRight size={18}/> 다음 개발: 거래 등록 API와 계좌 잔액 집계</div></section>
          </div>
          <div className="footer-note">Built for learning with Java, Spring Boot & Codex Harness <span>·</span> AssetFlow</div>
        </main>
      </div>

      {modal && <div className="modal-backdrop" onMouseDown={(e) => { if (!saving && e.target === e.currentTarget) { setModal(null); setPassword(""); } }}><div className="modal-card" role="dialog" aria-modal="true" aria-labelledby="modal-title"><div className="modal-heading"><div><h2 id="modal-title">{modal === "login" ? "로그인" : modal === "register" ? "회원가입" : "새 계좌 추가"}</h2><p>{modal === "account" ? "계좌의 기본 정보를 입력해 주세요." : "이메일과 비밀번호로 내 자산을 관리하세요."}</p></div><button className="icon-button" disabled={saving} onClick={() => { setModal(null); setPassword(""); }} aria-label="닫기"><X size={20}/></button></div><form onSubmit={modal === "account" ? submitAccount : submitMember} className="modal-form">
        {error && <div className="error-banner" role="alert">{error}</div>}
        {modal !== "account" ? <><label>이메일<input type="email" autoComplete="username" value={email} onChange={e => setEmail(e.target.value)} placeholder="study@example.com" maxLength={255} required/></label>{modal === "register" && <label>닉네임<input value={memberName} onChange={e => setMemberName(e.target.value)} placeholder="홍길동" maxLength={80} required/></label>}<label>비밀번호<input type="password" autoComplete={modal === "register" ? "new-password" : "current-password"} minLength={modal === "register" ? 8 : undefined} maxLength={72} value={password} onChange={e => setPassword(e.target.value)} placeholder={modal === "register" ? "8자 이상 (최대 72바이트)" : "비밀번호"} required/></label><button type="button" className="outline-button" disabled={saving} onClick={() => { setModal(modal === "login" ? "register" : "login"); setError(""); setPassword(""); }}>{modal === "login" ? "처음이신가요? 회원가입" : "이미 계정이 있나요? 로그인"}</button></> : <><label>계좌 이름<input value={accountName} onChange={e => setAccountName(e.target.value)} placeholder="예: 주거래 입출금" maxLength={100} required/></label><label>계좌 유형<div className="select-wrap"><select value={accountType} onChange={e => setAccountType(e.target.value as AccountType)}><option value="CHECKING">입출금</option><option value="SAVINGS">저축</option><option value="CASH">현금</option></select><ChevronDown size={16}/></div></label><label>초기 잔액 (원)<input type="number" min="0" step="1" value={balance} onChange={e => setBalance(e.target.value)} placeholder="0" required/></label></>}
        <div className="modal-actions"><button type="button" className="outline-button" disabled={saving} onClick={() => { setModal(null); setPassword(""); }}>취소</button><button className="primary-button" type="submit" disabled={saving}>{saving ? "저장 중..." : modal === "login" ? "로그인" : modal === "register" ? "가입하기" : "계좌 등록"}</button></div>
      </form></div></div>}
    </div>
  );
}
