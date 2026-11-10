"use client";

import { FormEvent, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { ArrowDownRight, ArrowUpRight, BarChart3, ChevronDown, CircleHelp, CreditCard, LayoutDashboard, Plus, RefreshCw, ShieldCheck, Wallet, X } from "lucide-react";
import { Account, AccountType, ApiError, Member, assetApi } from "@/lib/api";

import ProfileEditor from "@/components/ProfileEditor";
import ModalFrame from "@/components/ModalFrame";

const money = (value: number, currency = "KRW") => new Intl.NumberFormat("ko-KR", { style: "currency", currency, minimumFractionDigits: 0, maximumFractionDigits: 2 }).format(value);
const accountLabels: Record<AccountType, string> = { CHECKING: "입출금", SAVINGS: "저축", CASH: "현금" };
const accountColors = ["purple", "orange", "blue", "green"];
export default function Home() {
  const [editingProfile, setEditingProfile] = useState(false);
  const [member, setMember] = useState<Member | null>(null);
  const memberId = member?.id ?? null;
  const nickname = member?.nickname ?? "";
  const sessionVersion = useRef(0);
  const [sessionLoading, setSessionLoading] = useState(true);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [loading, setLoading] = useState(true);
  const [accountLoadFailed, setAccountLoadFailed] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [modal, setModal] = useState<"login" | "register" | "account" | "rename" | null>(null);
  const [selectedAccount, setSelectedAccount] = useState<Account | null>(null);
  const [email, setEmail] = useState("");
  const [memberName, setMemberName] = useState("");
  const [password, setPassword] = useState("");
  const [accountName, setAccountName] = useState("");
  const [accountType, setAccountType] = useState<AccountType>("CHECKING");
  const [balance, setBalance] = useState("");

  function openAccount() {
    if (memberId !== null && loading) return;
    setError(""); setAccountName(""); setBalance(""); setAccountType("CHECKING"); setModal(memberId !== null ? "account" : "register");
  }

  const loadAccounts = useCallback(async () => {
    const version = sessionVersion.current;
    setLoading(true);
    setAccountLoadFailed(false);
    setError("");
    try {
      const result = await assetApi.listAccounts();
      if (version === sessionVersion.current) setAccounts(result);
    }
    catch (cause) { if (version === sessionVersion.current) { setAccountLoadFailed(true); setError(cause instanceof Error ? cause.message : "계좌 조회에 실패했습니다."); } }
    finally { if (version === sessionVersion.current) setLoading(false); }
  }, []);

  useEffect(() => {
    let active = true;
    const version = sessionVersion.current;
    // 로컬 ID 대신 HttpOnly 쿠키로 현재 회원과 계좌를 복원한다.
    void assetApi.me().then(async current => {
      if (!active || version !== sessionVersion.current) return;
      setMember(current);
      await loadAccounts();
    }).catch(cause => {
      if (active && version === sessionVersion.current && !(cause instanceof ApiError && cause.status === 401)) {
        setError(cause instanceof Error ? cause.message : "로그인 상태를 확인하지 못했습니다.");
      }
    }).finally(() => { if (active) { setSessionLoading(false); setLoading(false); } });
    const expire = () => {
      sessionVersion.current += 1;
      setEditingProfile(false); setMember(null); setAccounts([]); setSelectedAccount(null); setModal("login"); setPassword(""); setLoading(false); setSaving(false); setAccountLoadFailed(false);
      setError("로그인이 만료되었습니다. 다시 로그인해 주세요.");
    };
    window.addEventListener("assetflow:unauthorized", expire);
    return () => { active = false; window.removeEventListener("assetflow:unauthorized", expire); };
  }, [loadAccounts]);

  const total = useMemo(() => accounts.filter(a => a.currency === "KRW").reduce((sum, account) => sum + Number(account.balance), 0), [accounts]);
  const savings = useMemo(() => accounts.filter(a => a.type === "SAVINGS" && a.currency === "KRW").reduce((sum, a) => sum + Number(a.balance), 0), [accounts]);

  async function submitMember(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setSaving(true); setError("");
    try {
      if (modal === "register" && !memberName.trim()) {
        setError("닉네임은 공백만 입력할 수 없습니다."); return;
      }
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
      setMember(null); setAccounts([]); setPassword(""); setModal(null); setEditingProfile(false); setSelectedAccount(null); setLoading(false); setAccountLoadFailed(false);
    } catch (cause) { setError(cause instanceof Error ? cause.message : "로그아웃에 실패했습니다."); }
    finally { setSaving(false); }
  }

  async function submitAccount(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (memberId === null) return;
    const version = sessionVersion.current;
    if (!accountName.trim()) { setError("계좌 이름을 입력해 주세요."); return; }
    const amount = Number(balance);
    if (modal !== "rename" && (!/^\d{1,17}(\.\d{1,2})?$/.test(balance) || !Number.isFinite(amount) || amount < 0)) { setError("0 이상, 정수 최대 17자리·소수 최대 2자리의 금액을 입력해 주세요."); return; }
    setSaving(true); setError("");
    try {
      const updated = modal === "rename" && selectedAccount
        ? await assetApi.renameAccount(selectedAccount.id, accountName.trim())
        : await assetApi.createAccount(accountName.trim(), accountType, amount);
      if (version !== sessionVersion.current) return;
      setAccounts(current => modal === "rename" ? current.map(account => account.id === updated.id ? updated : account) : [updated, ...current]);
      setModal(null); setSelectedAccount(null);
      setAccountName(""); setBalance("");
    } catch (cause) { if (version === sessionVersion.current) setError(cause instanceof Error ? cause.message : "계좌 저장에 실패했습니다."); }
    finally { if (version === sessionVersion.current) setSaving(false); }
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
        <header className="topbar"><span className="crumb">내 워크스페이스 <span>/</span> <b>대시보드</b></span><div className="top-actions"><span className="env-pill"><span className="dot"/> LOCAL DEVELOPMENT</span>{member ? <><button className="profile" disabled={saving} onClick={() => { setError(""); setEditingProfile(true); }} aria-label={`${nickname}님 프로필 수정`} title="프로필 수정">{nickname.slice(0, 1)}</button><button className="outline-button" disabled={saving} onClick={() => void logout()}>로그아웃</button></> : <button className="outline-button" disabled={sessionLoading} onClick={() => { setError(""); setModal("login"); }}>로그인</button>}</div></header>
        <main className="content">
          <div className="page-heading"><div><div className="eyebrow">FINANCIAL OVERVIEW</div><h1>{nickname ? `${nickname}님의 자산 현황` : "내 자산, 한눈에 관리하세요"}</h1><p>자산의 흐름을 명확하게 파악하고 더 나은 재무 습관을 만들어 보세요.</p></div><button className="primary-button" disabled={sessionLoading || saving} onClick={openAccount}><Plus size={17}/>{memberId !== null ? "새 계좌 추가" : "시작하기"}</button></div>

          <div className="demo-banner"><CircleHelp size={18}/><span><b>로컬 학습 환경</b> — 로그인한 회원의 계좌만 표시합니다. 잔액은 현재 초기 잔액 기준입니다.</span></div>
          {error && !modal && <div className="error-banner" role="alert">{error}<button onClick={() => setError("")} aria-label="오류 닫기"><X size={16}/></button></div>}

          <section className="summary-grid" aria-label="자산 요약">
            <div className="stat-card main-stat"><div className="stat-head"><span>총 보유 자산 (KRW)</span><span className="stat-icon purple"><Wallet size={20}/></span></div><strong>{sessionLoading || loading || accountLoadFailed ? "—" : money(total)}</strong><div className="stat-foot"><span className="stat-tag">현재 기준</span> 원화 계좌 합계 · 외화 제외</div></div>
            <div className="stat-card"><div className="stat-head"><span>저축 계좌 잔액 (KRW)</span><span className="stat-icon orange"><ArrowUpRight size={20}/></span></div><strong>{sessionLoading || loading || accountLoadFailed ? "—" : money(savings)}</strong><div className="stat-foot">원화 저축 계좌에 등록된 자산</div></div>
            <div className="stat-card"><div className="stat-head"><span>등록된 계좌</span><span className="stat-icon blue"><CreditCard size={20}/></span></div><strong>{accounts.length}<span className="unit">개</span></strong><div className="stat-foot">입출금 · 저축 · 현금</div></div>
          </section>

          <div className="section-grid">
            <section className="panel accounts-panel" id="accounts"><div className="panel-heading"><div><h2>내 계좌</h2><p>연결된 자산을 한곳에서 관리하세요</p></div><button className="icon-button" title="새로고침" disabled={memberId === null || loading} onClick={() => memberId !== null && void loadAccounts()}><RefreshCw size={17} className={loading ? "spin" : ""}/></button></div>
              {sessionLoading ? <div className="empty-state"><RefreshCw className="spin" size={23}/><p>로그인 상태를 확인하는 중...</p></div>
                : memberId === null ? <div className="empty-state"><div className="empty-icon"><Wallet size={25}/></div><h3>로그인하고 자산을 관리하세요</h3><p>회원가입 후 나만의 계좌를 등록하고<br/>자산 현황을 확인할 수 있어요.</p><button className="outline-button" onClick={() => { setError(""); setModal("login"); }}>로그인</button></div>
                : loading ? <div className="empty-state"><RefreshCw className="spin" size={23}/><p>계좌 정보를 불러오는 중...</p></div>
                : accountLoadFailed ? <div className="empty-state"><CircleHelp size={25}/><h3>계좌를 불러오지 못했습니다</h3><p>연결 상태를 확인한 뒤 다시 시도해 주세요.</p><button className="outline-button" onClick={() => void loadAccounts()}>다시 시도</button></div>
                : accounts.length === 0 ? <div className="empty-state"><div className="empty-icon"><CreditCard size={25}/></div><h3>아직 등록된 계좌가 없어요</h3><p>첫 번째 계좌를 추가해 보세요.</p><button className="outline-button" onClick={openAccount}>계좌 추가하기</button></div>
                : <div className="account-list">{accounts.map((account, index) => <div key={account.id} className="account-row"><div className={`account-avatar ${accountColors[index % accountColors.length]}`}><Wallet size={20}/></div><div className="account-info"><strong>{account.name}</strong><span>{accountLabels[account.type]} · {account.currency}</span></div><strong className="account-amount">{money(Number(account.balance), account.currency)}</strong><button className="icon-button" disabled={saving} aria-label={`${account.name} 이름 변경`} title="계좌 이름 변경" onClick={() => { setSelectedAccount(account); setAccountName(account.name); setError(""); setModal("rename"); }}><CreditCard size={16}/></button></div>)}</div>}
              <div className="panel-footer"><span>{accounts.length}개의 계좌</span><button disabled={memberId === null || saving} onClick={openAccount}>계좌 추가 <Plus size={15}/></button></div>
            </section>

            <section className="panel next-panel"><div className="panel-heading"><div><h2>개발 로드맵</h2><p>이번 프로젝트의 단계별 구현 현황</p></div><span className="roadmap-label">PHASE 01</span></div><div className="roadmap-list"><div className="roadmap-item done"><span className="roadmap-marker">✓</span><div><strong>기본 프로젝트 구조</strong><p>FE / BE / DB / Harness</p></div><span className="roadmap-status">구현</span></div><div className="roadmap-item done"><span className="roadmap-marker">✓</span><div><strong>회원 및 계좌 API</strong><p>Spring Data JPA + REST API</p></div><span className="roadmap-status">구현</span></div><div className="roadmap-item done"><span className="roadmap-marker">✓</span><div><strong>보안과 인증</strong><p>Spring Security + JWT + CSRF</p></div><span className="roadmap-status">구현</span></div><div className="roadmap-item current"><span className="roadmap-marker">4</span><div><strong>거래 내역 관리</strong><p>입출금, 이체, 잔액 계산</p></div><span className="roadmap-status">다음 단계</span></div></div><div className="roadmap-note"><ArrowDownRight size={18}/> 다음 개발: 거래 등록 API와 계좌 잔액 집계</div></section>
          </div>
          <div className="footer-note">Built for learning with Java, Spring Boot & Codex Harness <span>·</span> AssetFlow</div>
        </main>
      </div>

      {editingProfile && member && <ProfileEditor member={member} onClose={() => setEditingProfile(false)} onSave={async nickname => { const version = sessionVersion.current; const updated = await assetApi.updateNickname(nickname); if (version !== sessionVersion.current) throw new Error("로그인 상태가 변경되었습니다."); setMember(updated); return updated; }}/>}
      {modal && <ModalFrame key={modal} labelledBy="modal-title" busy={saving} onClose={() => { setModal(null); setPassword(""); setError(""); }}>
        <div className="modal-heading"><div><h2 id="modal-title">{modal === "login" ? "로그인" : modal === "register" ? "회원가입" : modal === "rename" ? "계좌 이름 변경" : "새 계좌 추가"}</h2><p>{modal === "account" || modal === "rename" ? "계좌의 기본 정보를 입력해 주세요." : "이메일과 비밀번호로 내 자산을 관리하세요."}</p></div><button className="icon-button" disabled={saving} onClick={() => { setModal(null); setPassword(""); setError(""); }} aria-label="닫기"><X size={20}/></button></div>
        <form onSubmit={modal === "account" || modal === "rename" ? submitAccount : submitMember} className="modal-form"><fieldset disabled={saving} className="form-fields">
          {error && <div className="error-banner" role="alert">{error}</div>}
          {modal === "login" || modal === "register" ? <>
            <label>이메일<input type="email" autoComplete="username" value={email} onChange={e => setEmail(e.target.value)} placeholder="study@example.com" maxLength={255} required/></label>
            {modal === "register" && <label>닉네임<input value={memberName} onChange={e => setMemberName(e.target.value)} placeholder="홍길동" maxLength={80} required/></label>}
            <label>비밀번호<input type="password" autoComplete={modal === "register" ? "new-password" : "current-password"} minLength={modal === "register" ? 8 : undefined} maxLength={72} value={password} onChange={e => setPassword(e.target.value)} placeholder={modal === "register" ? "8자 이상 (최대 72바이트)" : "비밀번호"} required/></label>
            <button type="button" className="outline-button" onClick={() => { setModal(modal === "login" ? "register" : "login"); setError(""); setPassword(""); }}>{modal === "login" ? "처음이신가요? 회원가입" : "이미 계정이 있나요? 로그인"}</button>
          </> : <>
            <label>계좌 이름<input value={accountName} onChange={e => setAccountName(e.target.value)} placeholder="예: 주거래 입출금" maxLength={100} required/></label>
            {modal === "rename" && selectedAccount ? <p className="account-detail">{accountLabels[selectedAccount.type]} · {selectedAccount.currency} · {money(Number(selectedAccount.balance), selectedAccount.currency)}<br/>계좌 유형과 잔액은 변경할 수 없습니다.</p> : <>
              <label>계좌 유형<div className="select-wrap"><select value={accountType} onChange={e => setAccountType(e.target.value as AccountType)}><option value="CHECKING">입출금</option><option value="SAVINGS">저축</option><option value="CASH">현금</option></select><ChevronDown size={16}/></div></label>
              <label>초기 잔액 (원)<input type="number" min="0" step="0.01" value={balance} onChange={e => setBalance(e.target.value)} placeholder="0" required/></label>
            </>}
          </>}
          <div className="modal-actions"><button type="button" className="outline-button" onClick={() => { setModal(null); setPassword(""); setError(""); }}>취소</button><button className="primary-button" type="submit">{saving ? "저장 중..." : modal === "login" ? "로그인" : modal === "register" ? "가입하기" : modal === "rename" ? "이름 저장" : "계좌 등록"}</button></div>
        </fieldset></form>
      </ModalFrame>}
    </div>
  );
}
