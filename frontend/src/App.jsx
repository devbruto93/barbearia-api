import { useCallback, useEffect, useMemo, useState } from 'react';
import { ApiError, request, } from './api';
const SESSION_KEY = 'barbearia.session';
function localDate(date = new Date()) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}
function formatDateTime(value) {
    return new Date(value).toLocaleString('pt-BR', {
        weekday: 'short',
        day: '2-digit',
        month: 'short',
        hour: '2-digit',
        minute: '2-digit',
    });
}
function readableError(error) {
    if (error instanceof ApiError && error.status === 401)
        return 'Sua sessão expirou. Entre novamente.';
    return error instanceof Error ? error.message : 'Não foi possível concluir a operação.';
}
export default function App() {
    const [auth, setAuth] = useState(() => {
        try {
            const saved = sessionStorage.getItem(SESSION_KEY);
            return saved ? JSON.parse(saved) : null;
        }
        catch {
            sessionStorage.removeItem(SESSION_KEY);
            return null;
        }
    });
    const [profile, setProfile] = useState(null);
    const [barbers, setBarbers] = useState([]);
    const [appointments, setAppointments] = useState([]);
    const [customers, setCustomers] = useState([]);
    const [view, setView] = useState('agenda');
    const [loading, setLoading] = useState(false);
    const [feedback, setFeedback] = useState(null);
    const notify = useCallback((text, kind = 'success') => {
        setFeedback({ text, kind });
        window.setTimeout(() => setFeedback(null), 4500);
    }, []);
    const clearSession = useCallback(() => {
        sessionStorage.removeItem(SESSION_KEY);
        setAuth(null);
        setProfile(null);
        setAppointments([]);
        setCustomers([]);
        setView('agenda');
    }, []);
    const refresh = useCallback(async () => {
        if (!auth)
            return;
        setLoading(true);
        try {
            const barberRequest = request('/barbeiros', { token: auth.token });
            if (auth.role === 'ADMIN') {
                const [nextBarbers, nextAppointments, nextCustomers] = await Promise.all([
                    barberRequest,
                    request('/agendamentos', { token: auth.token }),
                    request('/clientes', { token: auth.token }),
                ]);
                setBarbers(nextBarbers);
                setAppointments(nextAppointments);
                setCustomers(nextCustomers);
                setProfile(null);
            }
            else {
                const [nextBarbers, nextAppointments, nextProfile] = await Promise.all([
                    barberRequest,
                    request('/agendamentos/me', { token: auth.token }),
                    request('/clientes/me', { token: auth.token }),
                ]);
                setBarbers(nextBarbers);
                setAppointments(nextAppointments);
                setProfile(nextProfile);
            }
        }
        catch (error) {
            notify(readableError(error), 'error');
            if (error instanceof ApiError && error.status === 401)
                clearSession();
        }
        finally {
            setLoading(false);
        }
    }, [auth, clearSession, notify]);
    useEffect(() => {
        if (auth)
            void refresh();
    }, [auth, refresh]);
    async function signIn(email, password, registration) {
        try {
            const endpoint = registration ? '/auth/cadastro' : '/auth/login';
            const body = registration
                ? { email, senha: password, nome: registration.name }
                : { email, senha: password };
            const session = await request(endpoint, { method: 'POST', body });
            sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
            setAuth(session);
            setView('agenda');
            notify(registration ? 'Conta criada. Bem-vindo à barbearia.' : 'Acesso realizado.');
        }
        catch (error) {
            notify(readableError(error), 'error');
        }
    }
    async function signOut() {
        if (auth) {
            try {
                await request('/auth/logout', { token: auth.token, method: 'POST' });
            }
            catch {
                // A sessao local e encerrada mesmo se a API nao estiver disponivel.
            }
        }
        clearSession();
    }
    async function perform(action, success, reload = true) {
        try {
            const result = await action();
            notify(success);
            if (reload)
                await refresh();
            return result;
        }
        catch (error) {
            notify(readableError(error), 'error');
            throw error;
        }
    }
    if (!auth) {
        return <AuthScreen onSubmit={signIn} feedback={feedback}/>;
    }
    const isAdmin = auth.role === 'ADMIN';
    const displayName = profile?.nome || auth.email.split('@')[0];
    return (<div className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="#agenda" onClick={() => setView('agenda')}>
          <span className="brand-mark">B</span>
          <span><strong>barbearia</strong><small>AGENDA & ATENDIMENTO</small></span>
        </a>

        <div className="side-label">MENU PRINCIPAL</div>
        <nav className="side-nav" aria-label="Menu principal">
          <button className={view === 'agenda' ? 'nav-item active' : 'nav-item'} onClick={() => setView('agenda')}>
            <span className="nav-icon">▦</span> {isAdmin ? 'Agenda geral' : 'Minha agenda'}
          </button>
          {isAdmin && (<>
              <button className={view === 'barbeiros' ? 'nav-item active' : 'nav-item'} onClick={() => setView('barbeiros')}>
                <span className="nav-icon">✂</span> Barbeiros
              </button>
              <button className={view === 'clientes' ? 'nav-item active' : 'nav-item'} onClick={() => setView('clientes')}>
                <span className="nav-icon">♙</span> Clientes
              </button>
            </>)}
        </nav>

        <div className="sidebar-bottom">
          <div className="sidebar-note">
            <span className="tiny-mark">✦</span>
            <p>Um bom corte<br /><strong>começa na agenda.</strong></p>
          </div>
          <button className="profile-button" onClick={() => void signOut()}>
            <span className="avatar">{displayName.slice(0, 1).toUpperCase()}</span>
            <span className="profile-copy"><strong>{displayName}</strong><small>{isAdmin ? 'Administrador' : 'Cliente'}</small></span>
            <span className="logout-mark" aria-label="Sair">↗</span>
          </button>
        </div>
      </aside>

      <main className="main-area">
        <header className="topbar">
          <div className="breadcrumb"><span>BARBEARIA</span><i>/</i><strong>{viewLabel(view, isAdmin)}</strong></div>
          <div className="topbar-actions">
            <span className="today-label">{new Date().toLocaleDateString('pt-BR', { weekday: 'long', day: '2-digit', month: 'long' })}</span>
            <button className="icon-button" onClick={() => void refresh()} title="Atualizar agenda" aria-label="Atualizar agenda">↻</button>
          </div>
        </header>

        <div className="content-wrap">
          {feedback && <div className={`toast ${feedback.kind}`} role="status">{feedback.text}</div>}
          {loading && <div className="sync-indicator"><span className="spinner"/> Sincronizando com a API…</div>}

          {view === 'agenda' && isAdmin && (<AdminAgenda appointments={appointments} barbers={barbers} token={auth.token} onAction={perform} notify={notify}/>)}
          {view === 'agenda' && !isAdmin && (<CustomerAgenda appointments={appointments} barbers={barbers} token={auth.token} name={displayName} onAction={perform} notify={notify}/>)}
          {view === 'barbeiros' && isAdmin && (<BarberManagement barbers={barbers} token={auth.token} onAction={perform}/>)}
          {view === 'clientes' && isAdmin && <CustomerManagement customers={customers}/>}

          <footer className="page-footer"><span>BARBEARIA • SISTEMA DE AGENDAMENTO</span><span>Feito para cuidar dos detalhes.</span></footer>
        </div>
      </main>
    </div>);
}
function viewLabel(view, isAdmin) {
    if (view === 'barbeiros')
        return 'BARBEIROS';
    if (view === 'clientes')
        return 'CLIENTES';
    return isAdmin ? 'AGENDA GERAL' : 'MINHA AGENDA';
}
function AuthScreen({ onSubmit, feedback, }) {
    const [registration, setRegistration] = useState(false);
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [name, setName] = useState('');
    const [submitting, setSubmitting] = useState(false);
    async function submit(event) {
        event.preventDefault();
        setSubmitting(true);
        try {
            await onSubmit(email, password, registration ? { name } : undefined);
        }
        finally {
            setSubmitting(false);
        }
    }
    return (<div className="auth-layout">
      <section className="auth-story">
        <a className="brand light-brand" href="#inicio"><span className="brand-mark">B</span><span><strong>barbearia</strong><small>AGENDA & ATENDIMENTO</small></span></a>
        <div className="story-copy">
          <span className="eyebrow light-eyebrow">SEU MOMENTO, NO SEU TEMPO</span>
          <h1>O próximo<br />bom corte<br /><em>começa aqui.</em></h1>
          <p>Encontre seu barbeiro, escolha o melhor horário e deixe o resto com a gente.</p>
        </div>
        <div className="story-footer"><span>✦</span> Atendimento com hora marcada, sem complicação.</div>
        <div className="story-number">01 <i>—</i> 03</div>
      </section>

      <section className="auth-form-side">
        <div className="mobile-brand"><span className="brand-mark">B</span><strong>barbearia</strong></div>
        <div className="auth-form-wrap">
          <span className="eyebrow">{registration ? 'COMECE POR AQUI' : 'BEM-VINDO DE VOLTA'}</span>
          <h2>{registration ? 'Crie sua conta' : 'Entre na sua conta'}</h2>
          <p className="form-intro">{registration ? 'Seus próximos horários ficam a um passo.' : 'Acesse sua agenda e escolha seu próximo horário.'}</p>

          {feedback && <div className={`form-message ${feedback.kind}`} role="alert">{feedback.text}</div>}

          <form onSubmit={submit} className="auth-form">
            {registration && <label>Seu nome<input value={name} onChange={event => setName(event.target.value)} required minLength={3} maxLength={100} autoComplete="name" placeholder="Como podemos te chamar?"/></label>}
            <label>Email<input type="email" value={email} onChange={event => setEmail(event.target.value)} required maxLength={150} autoComplete="email" placeholder="voce@email.com"/></label>
            <label>Senha<input type="password" value={password} onChange={event => setPassword(event.target.value)} required minLength={registration ? 8 : 1} maxLength={72} autoComplete={registration ? 'new-password' : 'current-password'} placeholder="••••••••••"/></label>
            {registration && <small className="password-hint">Use 8 a 72 caracteres, com maiúscula, minúscula, número e símbolo.</small>}
            <button className="button primary auth-submit" disabled={submitting}>{submitting ? 'Aguarde…' : registration ? 'Criar conta' : 'Entrar'} <span>→</span></button>
          </form>

          <div className="auth-switch">{registration ? 'Já tem uma conta?' : 'Ainda não tem uma conta?'} <button onClick={() => setRegistration(!registration)}>{registration ? 'Entrar' : 'Criar conta'}</button></div>
          <div className="privacy-caption"><span>◈</span> Seus dados são tratados com cuidado e segurança.</div>
        </div>
        <div className="auth-copyright">© {new Date().getFullYear()} BARBEARIA. TODOS OS DIREITOS RESERVADOS.</div>
      </section>
    </div>);
}
function CustomerAgenda({ appointments, barbers, token, name, onAction, notify, }) {
    const [barberId, setBarberId] = useState('');
    const [day, setDay] = useState(localDate());
    const [slots, setSlots] = useState([]);
    const [selectedSlot, setSelectedSlot] = useState('');
    const [loadingSlots, setLoadingSlots] = useState(false);
    const [rescheduling, setRescheduling] = useState(null);
    const upcoming = useMemo(() => appointments.filter(item => item.status === 'AGENDADO')
        .sort((a, b) => a.dataHora.localeCompare(b.dataHora)), [appointments]);
    const history = useMemo(() => appointments.filter(item => item.status !== 'AGENDADO')
        .sort((a, b) => b.dataHora.localeCompare(a.dataHora)), [appointments]);
    useEffect(() => {
        setSelectedSlot('');
        if (!barberId || !day) {
            setSlots([]);
            return;
        }
        let active = true;
        setLoadingSlots(true);
        request(`/barbeiros/${barberId}/disponibilidade?data=${day}`, { token })
            .then(result => { if (active)
            setSlots(result); })
            .catch(error => { if (active)
            notify(readableError(error), 'error'); })
            .finally(() => { if (active)
            setLoadingSlots(false); });
        return () => { active = false; };
    }, [barberId, day, token, notify]);
    async function book() {
        if (!barberId || !selectedSlot)
            return;
        await onAction(() => request('/agendamentos', {
            token,
            method: 'POST',
            body: { barbeiroId: Number(barberId), dataHora: selectedSlot },
        }), 'Horário reservado com sucesso.');
        setSelectedSlot('');
    }
    async function answerNotice(id, decision, move) {
        try {
            await onAction(() => request(`/agendamentos/${id}/alteracao-horario`, {
                token,
                method: 'PATCH',
                body: { decisao: decision, ...(move ? { barbeiroId: move.barberId, dataHora: move.dateTime } : {}) },
            }), decision === 'ACEITAR' ? 'Novo horário aceito.' : move ? 'Agendamento remarcado.' : 'Pedido de remarcação enviado.');
            setRescheduling(null);
        }
        catch {
            // A mensagem de erro ja foi mostrada por onAction.
        }
    }
    async function cancel(id) {
        if (!window.confirm('Deseja cancelar este horário?'))
            return;
        try {
            await onAction(() => request(`/agendamentos/${id}/cancelar`, { token, method: 'PATCH' }), 'Agendamento cancelado.');
        }
        catch {
            // A mensagem de erro ja foi mostrada por onAction.
        }
    }
    return (<>
      <section className="welcome-row">
        <div><span className="eyebrow">{new Date().toLocaleDateString('pt-BR', { weekday: 'long' }).toUpperCase()} • UM BOM DIA PARA SE CUIDAR</span><h1>Olá, {name}<span className="title-period">.</span></h1><p>Escolha seu próximo horário com calma.</p></div>
        <div className="welcome-stamp"><span>✦</span><small>SEU PRÓXIMO<br />MOMENTO</small></div>
      </section>

      <section className="booking-layout">
        <div className="panel booking-panel">
          <div className="panel-heading"><div><span className="eyebrow">RESERVA RÁPIDA</span><h2>Marque seu horário</h2></div><span className="step-number">01 <i>/ 03</i></span></div>
          <div className="booking-fields">
            <label>Escolha o barbeiro
              <select value={barberId} onChange={event => setBarberId(event.target.value)}>
                <option value="">Selecione um profissional</option>
                {barbers.map(barber => <option key={barber.id} value={barber.id}>{barber.nome} — {barber.especialidade}</option>)}
              </select>
            </label>
            <label>Escolha o dia<input type="date" min={localDate()} value={day} onChange={event => setDay(event.target.value)}/></label>
          </div>
          <div className="slots-head"><strong>Horários disponíveis</strong><span>{day ? new Date(`${day}T12:00:00`).toLocaleDateString('pt-BR', { day: '2-digit', month: 'long' }) : 'Escolha um dia'}</span></div>
          {!barberId ? <div className="empty-slots">Escolha um barbeiro para consultar os horários.</div>
            : loadingSlots ? <div className="empty-slots"><span className="spinner"/> Buscando horários…</div>
                : slots.length ? <div className="slot-grid">{slots.map(slot => <button key={slot} className={selectedSlot === slot ? 'slot selected' : 'slot'} onClick={() => setSelectedSlot(slot)}>{slot.slice(11, 16)}</button>)}</div>
                    : <div className="empty-slots">Não há horários livres nessa data. Tente outro dia.</div>}
          <div className="booking-footer"><span><i className="green-dot"/> Duração padrão: 30 minutos</span><button className="button primary" disabled={!selectedSlot} onClick={() => void book()}>Confirmar horário <span>→</span></button></div>
        </div>
        <aside className="booking-aside"><div className="aside-art"><span className="art-star">✦</span><div className="art-circle"><span>B</span></div><div className="art-lines"/></div><span className="eyebrow">UM TEMPO PARA VOCÊ</span><p>Seu cuidado também merece um lugar na agenda.</p></aside>
      </section>

      <section className="section-block">
        <div className="section-title"><div><span className="eyebrow">SEUS PRÓXIMOS PASSOS</span><h2>Próximos horários <span className="count-pill">{upcoming.length}</span></h2></div><span className="section-caption">Sua agenda, sempre à mão.</span></div>
        {upcoming.length ? <div className="appointment-list">{upcoming.map(item => (<article className="appointment-card" key={item.id}>
            <div className="appointment-date"><strong>{new Date(item.dataHora).toLocaleDateString('pt-BR', { day: '2-digit' })}</strong><span>{new Date(item.dataHora).toLocaleDateString('pt-BR', { month: 'short' }).replace('.', '').toUpperCase()}</span></div>
            <div className="appointment-details"><strong>{item.barbeiroNome}</strong><span>{formatDateTime(item.dataHora)} <i>·</i> {item.duracaoMinutos} min</span></div>
            <span className="status-chip scheduled"><i /> Agendado</span>
            {item.alteracaoHorarioPendente && <span className="status-chip pending">Ação necessária</span>}
            <button className="text-button danger-text" onClick={() => void cancel(item.id)}>Cancelar</button>
          </article>))}</div> : <div className="empty-state"><span className="empty-icon">◷</span><strong>Nenhum horário marcado ainda</strong><p>Escolha um barbeiro acima e reserve seu próximo atendimento.</p></div>}
      </section>

      {upcoming.filter(item => item.alteracaoHorarioPendente).map(item => (<section className="notice-card" key={`notice-${item.id}`}>
          <div className="notice-icon">!</div>
          <div className="notice-copy"><span className="eyebrow">CONFIRMAÇÃO DE HORÁRIO</span><h3>Uma atualização sobre seu atendimento</h3><p>{item.avisoAlteracaoHorario}</p>
            {rescheduling === item.id ? <TimeMoveForm token={token} barbers={barbers} defaultBarberId={item.barbeiroId} defaultDate={item.dataHora.slice(0, 10)} onCancel={() => setRescheduling(null)} onConfirm={(newBarberId, newDateTime) => answerNotice(item.id, 'REMARCAR', { barberId: newBarberId, dateTime: newDateTime })}/> : <div className="notice-actions"><button className="button primary small" onClick={() => void answerNotice(item.id, 'ACEITAR')}>Aceitar horário</button><button className="button secondary small" onClick={() => setRescheduling(item.id)}>Escolher outro horário</button><button className="subtle-button" onClick={() => void answerNotice(item.id, 'REMARCAR')}>Preciso de ajuda para remarcar</button></div>}
          </div>
        </section>))}

      <section className="section-block history-section"><div className="section-title"><div><span className="eyebrow">SEU HISTÓRICO</span><h2>Atendimentos anteriores</h2></div></div>
        {history.length ? <div className="history-list">{history.slice(0, 5).map(item => <div className="history-row" key={item.id}><span className={`history-status ${item.status.toLowerCase()}`}/> <strong>{item.barbeiroNome}</strong><span>{formatDateTime(item.dataHora)}</span><em>{item.status === 'CONCLUIDO' ? 'Concluído' : 'Cancelado'}</em></div>)}</div> : <p className="muted-copy">Seus atendimentos concluídos e cancelados aparecerão aqui.</p>}
      </section>
    </>);
}
function TimeMoveForm({ token, barbers, defaultBarberId, defaultDate, onCancel, onConfirm, }) {
    const [barberId, setBarberId] = useState(String(defaultBarberId));
    const [date, setDate] = useState(defaultDate);
    const [slots, setSlots] = useState([]);
    const [selected, setSelected] = useState('');
    const [loading, setLoading] = useState(false);
    useEffect(() => {
        if (!barberId || !date)
            return;
        let active = true;
        setLoading(true);
        setSelected('');
        request(`/barbeiros/${barberId}/disponibilidade?data=${date}`, { token })
            .then(value => { if (active)
            setSlots(value); })
            .catch(() => { if (active)
            setSlots([]); })
            .finally(() => { if (active)
            setLoading(false); });
        return () => { active = false; };
    }, [barberId, date, token]);
    return <div className="move-form"><div className="move-fields"><label>Barbeiro<select value={barberId} onChange={event => setBarberId(event.target.value)}>{barbers.map(barber => <option key={barber.id} value={barber.id}>{barber.nome}</option>)}</select></label><label>Data<input type="date" min={localDate()} value={date} onChange={event => setDate(event.target.value)}/></label></div>
    <div className="move-slots">{loading ? <span className="muted-copy">Buscando horários…</span> : slots.length ? slots.map(slot => <button type="button" key={slot} className={selected === slot ? 'slot selected compact-slot' : 'slot compact-slot'} onClick={() => setSelected(slot)}>{slot.slice(11, 16)}</button>) : <span className="muted-copy">Nenhum horário livre nesse dia.</span>}</div>
    <div className="move-actions"><button type="button" className="button primary small" disabled={!selected} onClick={() => void onConfirm(Number(barberId), selected)}>Confirmar remarcação</button><button type="button" className="text-button" onClick={onCancel}>Voltar</button></div>
  </div>;
}
function AdminAgenda({ appointments, barbers, token, onAction, notify, }) {
    const [extraMinutes, setExtraMinutes] = useState({});
    const [moving, setMoving] = useState(null);
    const active = appointments.filter(item => item.status === 'AGENDADO').sort((a, b) => a.dataHora.localeCompare(b.dataHora));
    const completedCount = appointments.filter(item => item.status === 'CONCLUIDO').length;
    async function addTime(item) {
        const minutes = Number(extraMinutes[item.id] ?? 15);
        if (!Number.isInteger(minutes) || minutes < 1 || minutes > 240) {
            notify('Informe um acréscimo entre 1 e 240 minutos.', 'error');
            return;
        }
        try {
            await onAction(() => request(`/agendamentos/${item.id}/adicionar-tempo`, {
                token, method: 'PATCH', body: { minutos: minutes },
            }), `A duração foi ampliada em ${minutes} minutos. Os próximos clientes foram avisados.`);
        }
        catch {
            // A mensagem de erro ja foi mostrada.
        }
    }
    async function moveAppointment(item, barberId, dateTime) {
        try {
            await onAction(() => request(`/agendamentos/${item.id}`, {
                token,
                method: 'PUT',
                body: { barbeiroId: barberId, dataHora: dateTime, clienteId: item.clienteId },
            }), 'Agendamento remarcado.');
            setMoving(null);
        }
        catch {
            // A mensagem de erro ja foi mostrada.
        }
    }
    async function complete(item) {
        try {
            await onAction(() => request(`/agendamentos/${item.id}/concluir`, { token, method: 'PATCH' }), 'Atendimento concluído.');
        }
        catch {
            // A mensagem de erro ja foi mostrada.
        }
    }
    return <>
    <section className="welcome-row admin-welcome"><div><span className="eyebrow">PAINEL DA BARBEARIA</span><h1>Agenda geral<span className="title-period">.</span></h1><p>Organize os atendimentos e cuide de cada detalhe.</p></div><div className="welcome-stamp admin-stamp"><span>✂</span><small>VISÃO<br />DO DIA</small></div></section>
    <section className="stats-grid"><div className="stat-card"><span className="eyebrow">AGENDADOS</span><strong>{active.length.toString().padStart(2, '0')}</strong><small>atendimentos ativos</small></div><div className="stat-card"><span className="eyebrow">BARBEIROS ATIVOS</span><strong>{barbers.filter(barber => barber.ativo).length.toString().padStart(2, '0')}</strong><small>profissionais na agenda</small></div><div className="stat-card"><span className="eyebrow">CONCLUÍDOS</span><strong>{completedCount.toString().padStart(2, '0')}</strong><small>atendimentos realizados</small></div><div className="stat-card stat-note"><span>✦</span><p>Agenda organizada,<br /><strong>dia mais tranquilo.</strong></p></div></section>

    <section className="section-block admin-appointments"><div className="section-title"><div><span className="eyebrow">ACOMPANHAMENTO</span><h2>Próximos atendimentos <span className="count-pill">{active.length}</span></h2></div><span className="section-caption">Todos os horários ativos.</span></div>
      {active.length ? <div className="admin-list">{active.map(item => <article className="admin-appointment" key={item.id}>
        <div className="admin-date-block"><strong>{new Date(item.dataHora).toLocaleDateString('pt-BR', { day: '2-digit' })}</strong><span>{new Date(item.dataHora).toLocaleDateString('pt-BR', { month: 'short' }).replace('.', '').toUpperCase()}</span></div>
        <div className="admin-client"><strong>{item.clienteNome}</strong><span>{formatDateTime(item.dataHora)}</span></div>
        <div className="admin-barber"><span>BARBEIRO</span><strong>{item.barbeiroNome}</strong></div>
        <div className="admin-duration"><span>DURAÇÃO</span><strong>{item.duracaoMinutos} min</strong></div>
        {item.remarcacaoSolicitada && <span className="status-chip pending">Pediu remarcação</span>}
        <div className="admin-controls">
          <div className="extend-control"><input aria-label="Minutos adicionais" type="number" min="1" max="240" value={extraMinutes[item.id] ?? '15'} onChange={event => setExtraMinutes(current => ({ ...current, [item.id]: event.target.value }))}/><button className="button secondary small" onClick={() => void addTime(item)}>+ Tempo</button></div>
          {item.remarcacaoSolicitada && <button className="text-button" onClick={() => setMoving(moving === item.id ? null : item.id)}>Remarcar cliente</button>}
          <button className="text-button complete-button" onClick={() => void complete(item)}>Concluir</button>
        </div>
        {moving === item.id && <div className="admin-move-panel"><span className="eyebrow">NOVO HORÁRIO PARA {item.clienteNome.toUpperCase()}</span><TimeMoveForm token={token} barbers={barbers} defaultBarberId={item.barbeiroId} defaultDate={item.dataHora.slice(0, 10)} onCancel={() => setMoving(null)} onConfirm={(newBarberId, dateTime) => moveAppointment(item, newBarberId, dateTime)}/></div>}
      </article>)}</div> : <div className="empty-state"><span className="empty-icon">◷</span><strong>Agenda livre por enquanto</strong><p>Os próximos atendimentos aparecerão aqui.</p></div>}
    </section>
  </>;
}
function BarberManagement({ barbers, token, onAction, }) {
    const [name, setName] = useState('');
    const [phone, setPhone] = useState('');
    const [specialty, setSpecialty] = useState('');
    async function createBarber(event) {
        event.preventDefault();
        try {
            await onAction(() => request('/barbeiros', { token, method: 'POST', body: { nome: name, telefone: phone.replace(/\D/g, ''), especialidade: specialty } }), 'Barbeiro cadastrado.');
            setName('');
            setPhone('');
            setSpecialty('');
        }
        catch {
            // A mensagem de erro ja foi mostrada.
        }
    }
    async function deactivate(barber) {
        if (!window.confirm(`Desativar ${barber.nome}? O histórico será preservado. Horários ativos precisam ser remanejados antes.`))
            return;
        try {
            await onAction(() => request(`/barbeiros/${barber.id}`, { token, method: 'DELETE' }), 'Barbeiro desativado.');
        }
        catch {
            // A mensagem de erro ja foi mostrada.
        }
    }
    return <>
    <section className="welcome-row admin-welcome"><div><span className="eyebrow">EQUIPE</span><h1>Barbeiros<span className="title-period">.</span></h1><p>Profissionais ativos e informações de atendimento.</p></div><div className="welcome-stamp"><span>✂</span><small>TIME<br />DA CASA</small></div></section>
    <section className="management-layout"><div className="panel create-panel"><span className="eyebrow">NOVO PROFISSIONAL</span><h2>Adicionar barbeiro</h2><form className="stacked-form" onSubmit={event => void createBarber(event)}><label>Nome<input required minLength={3} maxLength={100} value={name} onChange={event => setName(event.target.value)} placeholder="Nome completo"/></label><label>Telefone<input required minLength={10} maxLength={11} inputMode="numeric" value={phone} onChange={event => setPhone(event.target.value.replace(/\D/g, '').slice(0, 11))} placeholder="Somente números"/></label><label>Especialidade<input required minLength={3} maxLength={50} value={specialty} onChange={event => setSpecialty(event.target.value)} placeholder="Ex.: Corte clássico"/></label><button className="button primary">Cadastrar profissional <span>→</span></button></form></div>
      <div className="team-list"><div className="section-title compact-title"><div><span className="eyebrow">PROFISSIONAIS</span><h2>Equipe ativa <span className="count-pill">{barbers.length}</span></h2></div></div>{barbers.map(barber => <article className="team-card" key={barber.id}><span className="team-avatar">{barber.nome.slice(0, 1).toUpperCase()}</span><div><strong>{barber.nome}</strong><span>{barber.especialidade}</span><small>{barber.telefone}</small></div><button className="text-button danger-text" onClick={() => void deactivate(barber)}>Desativar</button></article>)}{!barbers.length && <div className="empty-state small-empty"><strong>Nenhum barbeiro cadastrado</strong><p>Adicione o primeiro profissional da equipe.</p></div>}</div>
    </section>
  </>;
}
function CustomerManagement({ customers }) {
    return <>
    <section className="welcome-row admin-welcome"><div><span className="eyebrow">RELACIONAMENTO</span><h1>Clientes<span className="title-period">.</span></h1><p>Visão dos perfis cadastrados na barbearia.</p></div><div className="welcome-stamp"><span>♙</span><small>CLIENTES<br />DA CASA</small></div></section>
    <section className="section-block customer-table-section"><div className="section-title"><div><span className="eyebrow">CADASTROS</span><h2>Lista de clientes <span className="count-pill">{customers.length}</span></h2></div></div>{customers.length ? <div className="customer-table"><div className="table-header"><span>CLIENTE</span><span>TELEFONE</span><span>PERFIL</span></div>{customers.map(customer => <div className="table-row" key={customer.id}><span className="customer-name"><i>{customer.nome.slice(0, 1).toUpperCase()}</i><strong>{customer.nome}</strong></span><span>{customer.telefone || 'Não informado'}</span><span className="status-chip scheduled"><i /> Ativo</span></div>)}</div> : <div className="empty-state"><strong>Nenhum cliente cadastrado</strong><p>Os novos cadastros aparecerão aqui.</p></div>}</section>
  </>;
}
