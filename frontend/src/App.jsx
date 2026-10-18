import { useEffect, useMemo, useRef, useState } from 'react'

const API = 'https://codejar-production.up.railway.app/api'
const STARTERS = {
  JAVA: `import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        // Write your solution here

    }
}`,
  PYTHON: '# Write your solution here\n'
}

const getPath = () => window.location.pathname
const normalizeLanguage = (value = 'JAVA') => {
  const normalized = String(value ?? 'JAVA').trim().toUpperCase()
  return normalized === 'PYTHON' ? 'PYTHON' : 'JAVA'
}
const lang = (path) => normalizeLanguage(path.split('/')[2])
const LANGUAGE_META = {
  JAVA: { label: 'Java', fileName: 'Main.java', icon: 'JV', editor: 'java', starter: STARTERS.JAVA },
  PYTHON: { label: 'Python', fileName: 'main.py', icon: 'PY', editor: 'python', starter: STARTERS.PYTHON }
}

export default function App() {
  const [token, setToken] = useState(() => localStorage.getItem('codejar_token'))
  const [user, setUser] = useState(() => JSON.parse(localStorage.getItem('codejar_user') || 'null'))
  const [path, setPath] = useState(getPath)
  const [theme, setTheme] = useState(() => localStorage.getItem('codejar_theme') === 'light' ? 'light' : 'dark')
  const [difficulty, setDifficulty] = useState('EASY')
  const [form, setForm] = useState({ name: '', email: '', password: '' })
  const [message, setMessage] = useState('')
  const [dash, setDash] = useState()
  const [profile, setProfile] = useState(null)
  const [problems, setProblems] = useState([])
  const [problem, setProblem] = useState()
  const [code, setCode] = useState(STARTERS.JAVA)
  const [input, setInput] = useState('')
  const [result, setResult] = useState({ output: 'Choose a problem and run your code.', error: '' })
  const [resultModal, setResultModal] = useState(null)
  const [busy, setBusy] = useState(false)
  const [history, setHistory] = useState([])
  const [adminData, setAdminData] = useState({ problems: [], users: [], submissions: [] })
  const draftTimerRef = useRef(null)
  const rootLoginRef = useRef(getPath() === '/')

  const routeLanguage = lang(path)
  const language = normalizeLanguage(problem?.language || routeLanguage)
  const languageMeta = LANGUAGE_META[language]
  const headers = useMemo(() => ({ 'Content-Type': 'application/json', Authorization: `Bearer ${token}` }), [token])

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    localStorage.setItem('codejar_theme', theme)
  }, [theme])

  useEffect(() => {
    const onPopState = () => setPath(getPath())
    window.addEventListener('popstate', onPopState)
    return () => window.removeEventListener('popstate', onPopState)
  }, [])

  function go(next, replace = false) {
    window.history[replace ? 'replaceState' : 'pushState']({}, '', next)
    setPath(next)
  }

  useEffect(() => {
    const pub = ['/login', '/register', '/admin/login'].includes(path)
    if (path === '/') {
      rootLoginRef.current = true
      go('/login', true)
      return
    }
    if (!token && !pub) go(path.startsWith('/admin') ? '/admin/login' : '/login', true)
    if (token && path.startsWith('/admin') && user?.role !== 'ADMIN') go('/dashboard', true)
    if (token && path === '/admin/login' && user?.role === 'ADMIN') go('/admin/dashboard', true)
    if (token && ['/login', '/register'].includes(path) && !(path === '/login' && rootLoginRef.current)) go(user?.role === 'ADMIN' ? '/admin/dashboard' : '/dashboard', true)
  }, [token, path])

  useEffect(() => {
    if (!token) return
    if (path === '/dashboard') loadDash()
    if (path === '/history') loadHistory()
    if (path === '/profile') loadProfile()
    if (path === '/admin/dashboard' && user?.role === 'ADMIN') loadAdminData()
    if (path.startsWith('/language/')) loadProblems(routeLanguage)
    if (path.startsWith('/problems/')) loadProblems(language)
  }, [token, path, routeLanguage, language, difficulty])

  useEffect(() => {
    if (path.startsWith('/language/')) {
      setProblem(null)
      setCode(STARTERS[routeLanguage] || STARTERS.JAVA)
      setInput('')
      setResult({ output: 'Choose a problem and run your code.', error: '' })
    }
  }, [path, routeLanguage])

  async function req(url, options = {}) {
    const response = await fetch(API + url, { ...options, headers: { ...headers, ...options.headers } })
    const text = await response.text()
    const body = text ? JSON.parse(text) : null
    if (!response.ok) {
      if (response.status === 401) logout()
      throw Error(body?.message || 'Request failed.')
    }
    return body
  }

  async function loadDash() {
    try {
      setDash(await req('/dashboard'))
    } catch (e) {
      setMessage(e.message)
    }
  }

  async function loadHistory() {
    try {
      setHistory(await req('/submissions'))
    } catch (e) {
      setMessage(e.message)
    }
  }

  async function loadProfile() {
    try {
      setProfile(await req('/user/profile'))
    } catch (e) {
      setMessage(e.message)
    }
  }

  async function loadProblems(languageValue) {
    try {
      const data = await req(`/problems?language=${languageValue}&difficulty=${difficulty}`)
      setProblems(data)
      const id = path.startsWith('/problems/') ? path.split('/')[2] : null
      if (id && String(id) !== String(problem?.id)) selectProblem(id)
    } catch (e) {
      setMessage(e.message)
    }
  }

  async function loadAdminData() {
    try {
      const [adminProblems, users, submissions] = await Promise.all([
        req('/admin/problems'), req('/admin/users'), req('/admin/submissions')
      ])
      setAdminData({ problems: adminProblems, users, submissions })
    } catch (e) { setMessage(e.message) }
  }

  async function loadDraft(taskId) {
    if (!token) return null
    try {
      const response = await fetch(`${API}/code-drafts/${taskId}`, {
        method: 'GET',
        headers: { Authorization: `Bearer ${token}` }
      })
      if (!response.ok) return null
      if (response.status === 204) return null
      const data = await response.json()
      return data?.sourceCode ?? null
    } catch (e) {
      return null
    }
  }

  async function loadLatestSubmission(title, language) {
    const submissions = await req('/submissions')
    const latest = submissions.find(submission => submission.taskTitle === title && submission.language === language)
    return latest ? latest.sourceCode : null
  }

  async function selectProblem(id) {
    try {
      const data = await req(`/problems/${id}`)
      const nextLanguage = normalizeLanguage(data.language || language)
      const nextProblem = { ...data, language: nextLanguage }
      const [savedDraft, savedSubmission] = await Promise.all([loadDraft(id), loadLatestSubmission(data.title, nextLanguage)])
      setProblem(nextProblem)
      setCode(savedSubmission ?? savedDraft ?? (data.starterCode || STARTERS[nextLanguage]))
      setInput('')
      setResult({ output: 'Problem loaded. Write your solution, then run it.', error: '' })
      if (String(id) !== path.split('/')[2]) go(`/problems/${id}`)
    } catch (e) {
      setMessage(e.message)
    }
  }

  async function auth(e) {
    e.preventDefault()
    setMessage('')
    const login = path === '/login'
    const adminLogin = path === '/admin/login'
    try {
      const response = await fetch(`${API}/auth/${adminLogin ? 'admin-login' : login ? 'login' : 'register'}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(login ? { email: form.email, password: form.password } : form)
      })
      const data = await response.json()
      if (!response.ok) throw Error(data.message || 'Authentication failed.')
      if (!login && !adminLogin) {
        go('/login')
        setMessage('Registered successfully. Please log in.')
        return
      }
      localStorage.setItem('codejar_token', data.token)
      localStorage.setItem('codejar_user', JSON.stringify(data))
      setToken(data.token)
      setUser(data)
      go(adminLogin ? '/admin/dashboard' : '/dashboard')
    } catch (e) {
      setMessage(e.message)
    }
  }

  useEffect(() => {
    if (!token || !problem?.id) return

    if (draftTimerRef.current) clearTimeout(draftTimerRef.current)

    draftTimerRef.current = setTimeout(async () => {
      try {
        await fetch(`${API}/code-drafts/${problem.id}`, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`
          },
          body: JSON.stringify({ taskId: problem.id, language, sourceCode: code })
        })
      } catch (e) {
        console.error('Draft save failed:', e)
      }
    }, 700)

    return () => {
      if (draftTimerRef.current) clearTimeout(draftTimerRef.current)
    }
  }, [code, language, problem?.id, token])

  async function run() {
    setBusy(true)
    const controller = new AbortController()
    const timeoutId = setTimeout(() => controller.abort(), 15000)
    console.log('RUN START')

    try {
      console.log('REQUEST SENT', { language, inputLength: input.length, codeLength: code.length })
      const response = await fetch(`${API}/runner/run`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({ language, code, input }),
        signal: controller.signal
      })

      console.log('RESPONSE RECEIVED', response.status)
      const data = await response.json().catch(() => ({}))

      if (!response.ok) {
        throw new Error(data?.message || 'Run request failed.')
      }

      setResult({
        output: data.output || '',
        error: data.error || '',
        executionTime: data.executionTime,
        timedOut: !!data.timedOut
      })
    } catch (e) {
      console.log('RUN ERROR', e?.message || e)
      setResult({
        output: '',
        error: e?.name === 'AbortError' ? 'Request timed out after 15 seconds.' : (e?.message || 'Run request failed.')
      })
    } finally {
      clearTimeout(timeoutId)
      setBusy(false)
      console.log('RUN END')
    }
  }

  async function submit() {
    if (!problem) return
    setBusy(true)
    try {
      const data = await req('/submissions', {
        method: 'POST',
        body: JSON.stringify({ taskId: problem.id, language, code })
      })

      const nextModal = {
        type: data.status === 'ACCEPTED' ? 'success' : (data.status === 'PARTIALLY_ACCEPTED' ? 'warning' : 'error'),
        title: data.status === 'ACCEPTED' && Number(data.score) === 100 ? 'Already completed' : data.status.replaceAll('_', ' '),
        status: data.status,
        passedTests: data.passedTests,
        totalTests: data.totalTests,
        score: data.score,
        executionTime: data.executionTime,
        message: Number(data.score) === 100 && data.status === 'ACCEPTED' ? 'You have already solved this problem with 100%.' : ''
      }

      setResultModal(nextModal)
      setResult({
        output: `Status: ${data.status.replaceAll('_', ' ')}\nPassed: ${data.passedTests} / ${data.totalTests}\nScore: ${data.score}%`,
        error: '',
        executionTime: data.executionTime
      })
      loadDash()
      loadProblems(language)
    } catch (e) {
      setResultModal({
        type: 'error',
        title: 'Submission blocked',
        message: e.message,
        passedTests: 0,
        totalTests: 0,
        score: 0,
        executionTime: 0,
        status: 'ERROR'
      })
      setResult({ output: '', error: e.message })
    } finally {
      setBusy(false)
    }
  }

  function logout() {
    localStorage.removeItem('codejar_token')
    localStorage.removeItem('codejar_user')
    setToken(null)
    setUser(null)
    go('/login', true)
  }

  if (!token || (path === '/login' && rootLoginRef.current)) {
    const login = path === '/login'
    const adminLogin = path === '/admin/login'
    return (
      <main className={adminLogin ? 'auth-shell admin-auth-shell' : 'auth-shell'}>
        <section className="auth-card">
          <Brand />
          <p className="eyebrow">{adminLogin ? 'CODEJAR ADMINISTRATION' : 'LEARN BY BUILDING'}</p>
          <h1>{adminLogin ? 'Admin access.' : login ? 'Welcome back.' : 'Start your journey.'}</h1>
          <form onSubmit={auth}>
            {!login && !adminLogin && <input placeholder="Your name" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} required />}
            <label>
              Email
              <input type="email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} required />
            </label>
            <label>
              Password
              <input type="password" value={form.password} onChange={e => setForm({ ...form, password: e.target.value })} required />
            </label>
            <button className="run-button full">{adminLogin ? 'Admin login' : login ? 'Login' : 'Create account'}</button>
          </form>
          {message && <p className="form-message">{message}</p>}
          {!adminLogin && <button className="link-button" onClick={() => go(login ? '/register' : '/login')}>
            {login ? "Don't have an account? Register" : 'Already registered? Login'}
          </button>}
          {login && <button className="link-button" onClick={() => go('/admin/login')}>Admin Login</button>}
          {adminLogin && <button className="link-button" onClick={() => go('/login')}>Return to user login</button>}
        </section>
      </main>
    )
  }

  return (
    <main className="app-shell">
      <nav className="topbar">
        <button className="brand brand-button" onClick={() => go(user?.role === 'ADMIN' ? '/admin/dashboard' : '/dashboard')}>
          <span className="brand-mark">&lt;/&gt;</span>CodeJar<span className="brand-dot">.</span>
        </button>
        <div className="nav-links">
          {user?.role === 'ADMIN' ? <button onClick={() => go('/admin/dashboard')}>Admin Dashboard</button> : <><button onClick={() => go('/dashboard')}>Dashboard</button><button onClick={() => go('/history')}>History</button><button onClick={() => go('/profile')}>Profile</button></>}
        </div>
        <span className="user-name">{user?.name}</span>
        {user?.role !== 'ADMIN' && <button className="theme-toggle" onClick={() => setTheme(current => current === 'dark' ? 'light' : 'dark')} aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`} title={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}>{theme === 'dark' ? '☀' : '☾'}</button>}
        <button className="logout" onClick={logout}>Logout</button>
      </nav>

      {path === '/admin/dashboard' ? (
        <AdminDashboard data={adminData} req={req} reload={loadAdminData} />
      ) : path === '/dashboard' ? (
        <Dashboard d={dash} user={user} go={go} />
      ) : path === '/history' ? (
        <History h={history} go={go} />
      ) : path === '/profile' ? (
        <Profile 
          profile={profile} 
          user={user} 
          setUser={setUser}
          token={token}
          message={message}
          setMessage={setMessage}
          req={req}
        />
      ) : (
        <Workspace
          {...{
            language,
            languageMeta,
            difficulty,
            setDifficulty,
            problems,
            problem,
            code,
            setCode,
            input,
            setInput,
            result,
            resultModal,
            busy,
            selectProblem,
            run,
            submit,
            go,
            setResultModal
          }}
        />
      )}
    </main>
  )
}

function Brand() {
  return (
    <div className="brand">
      <span className="brand-mark">&lt;/&gt;</span>CodeJar<span className="brand-dot">.</span>
    </div>
  )
}

function Dashboard({ d, user, go }) {
  const card = (name, key) => {
    const x = d?.[key] || { percentage: 0, completed: 0, total: 0 }
    return (
      <button className="language-card" onClick={() => go(`/language/${key}`)} key={key}>
        <b>{name}</b>
        <strong>{x.percentage}% completed</strong>
        <div className="progress"><i style={{ width: `${x.percentage}%` }} /></div>
        <span>{x.completed} / {x.total} Problems</span>
        <em>View Problems →</em>
      </button>
    )
  }

  return (
    <section className="dashboard">
      <p className="eyebrow"><span /> STUDENT DASHBOARD</p>
      <h1>Welcome back, <em>{user?.name || 'Student'}</em></h1>
      <p className="hero-copy">Choose a language, solve challenges, and track your progress.</p>
      <p className="muted">🔥 {d?.streak ?? 0} Day Streak</p>
      <div className="stat-cards">
        {[['Completed', d?.completed ?? 0], ['Pending', d?.pending ?? 0], ['Average Score', `${d?.averageScore ?? 0}%`], ['Current Activity', d?.currentLanguage || '—']].map(x => (
          <article key={x[0]}>
            <span>{x[0]}</span>
            <b>{x[1]}</b>
          </article>
        ))}
      </div>
      <h2>Your Progress</h2>
      <div className="language-cards">
        {card('Java', 'java')}
        {card('Python', 'python')}
      </div>
      <h2>Recent submissions</h2>
      <div className="recent-list">
        {d?.recentSubmissions?.length ? d.recentSubmissions.map(s => (
          <article key={s.id}>
            <b>{s.taskTitle}</b>
            <span>{s.language} · {s.score}% · {s.status.replaceAll('_', ' ')}</span>
          </article>
        )) : <p className="muted">No submissions yet.</p>}
      </div>
    </section>
  )
}

function History({ h, go }) {
  return (
    <section className="history">
      <div className="workspace-head">
        <h2>Submission history</h2>
        <button className="run-button" onClick={() => go('/dashboard')}>New solution</button>
      </div>
      <div className="history-list">
        {h.length ? h.map(i => (
          <article className="history-item" key={i.id}>
            <div>
              <b>{i.taskTitle}</b>
              <small>{i.language} · {new Date(i.submittedAt).toLocaleString()}</small>
            </div>
            <span className={i.status === 'ACCEPTED' ? 'accepted' : 'failed'}>{i.status.replaceAll('_', ' ')}</span>
            <strong>{i.score}%</strong>
            <details>
              <summary>View submitted code</summary>
              <pre>{i.sourceCode}</pre>
            </details>
            <small>{i.passedTests}/{i.totalTests} tests · {i.executionTime} ms</small>
          </article>
        )) : <p className="muted">No submissions yet.</p>}
      </div>
    </section>
  )
}

function Profile({ profile, user, setUser, token, message, setMessage, req }) {
  const [editMode, setEditMode] = useState(false)
  const [editForm, setEditForm] = useState({ name: user?.name || '', email: user?.email || '' })
  const [pwForm, setPwForm] = useState({ oldPassword: '', newPassword: '', confirmPassword: '' })
  const [pwMessage, setPwMessage] = useState('')
  const [showPwForm, setShowPwForm] = useState(false)

  const handleEditProfile = async (e) => {
    e.preventDefault()
    try {
      const result = await req('/user/profile', {
        method: 'PUT',
        body: JSON.stringify({ name: editForm.name, email: editForm.email })
      })
      setUser(result.user)
      setEditMode(false)
      setMessage('Profile updated successfully!')
      setTimeout(() => setMessage(''), 3000)
    } catch (e) {
      setMessage(e.message)
    }
  }

  const handleChangePassword = async (e) => {
    e.preventDefault()
    if (pwForm.newPassword !== pwForm.confirmPassword) {
      setPwMessage('Passwords do not match')
      return
    }
    try {
      await req('/user/change-password', {
        method: 'POST',
        body: JSON.stringify({ oldPassword: pwForm.oldPassword, newPassword: pwForm.newPassword })
      })
      setPwForm({ oldPassword: '', newPassword: '', confirmPassword: '' })
      setShowPwForm(false)
      setPwMessage('Password changed successfully!')
      setTimeout(() => setPwMessage(''), 3000)
    } catch (e) {
      setPwMessage(e.message)
    }
  }

  const achievements = profile?.achievements || {}
  const stats = profile?.stats || {}
  const languages = profile?.languages || {}

  return (
    <section className="profile">
      <div className="workspace-head">
        <h2>Your Profile</h2>
      </div>

      {message && <p className="form-message success">{message}</p>}

      <div className="profile-grid">
        {/* User Info Card */}
        <article className="profile-card user-card">
          <header className="card-header">
            <h3>Account Information</h3>
            <button className="link-button" onClick={() => setEditMode(!editMode)}>
              {editMode ? 'Cancel' : 'Edit'}
            </button>
          </header>
          {editMode ? (
            <form onSubmit={handleEditProfile}>
              <label>
                Name
                <input
                  type="text"
                  value={editForm.name}
                  onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
                  required
                />
              </label>
              <label>
                Email
                <input
                  type="email"
                  value={editForm.email}
                  onChange={(e) => setEditForm({ ...editForm, email: e.target.value })}
                  required
                />
              </label>
              <button className="run-button full">Save changes</button>
            </form>
          ) : (
            <div className="user-info">
              <p><strong>Name:</strong> {user?.name}</p>
              <p><strong>Email:</strong> {user?.email}</p>
              <p><strong>Member since:</strong> {profile?.createdAt ? new Date(profile.createdAt).toLocaleDateString() : 'N/A'}</p>
            </div>
          )}
        </article>

        {/* Statistics Card */}
        <article className="profile-card stats-card">
          <header className="card-header">
            <h3>Statistics</h3>
          </header>
          <div className="stats-grid">
            <div className="stat">
              <span className="stat-label">Problems Solved</span>
              <strong className="stat-value">{stats.totalSolved || 0}</strong>
            </div>
            <div className="stat">
              <span className="stat-label">Total Attempts</span>
              <strong className="stat-value">{stats.totalAttempts || 0}</strong>
            </div>
            <div className="stat">
              <span className="stat-label">Average Score</span>
              <strong className="stat-value">{stats.averageScore || 0}%</strong>
            </div>
          </div>
        </article>

        {/* Language Breakdown */}
        <article className="profile-card languages-card">
          <header className="card-header">
            <h3>Language Progress</h3>
          </header>
          <div className="languages-breakdown">
            {languages.java && (
              <div className="language-item">
                <b>Java</b>
                <div className="progress">
                  <i style={{ width: `${languages.java.percentage}%` }} />
                </div>
                <span>{languages.java.completed} / {languages.java.total}</span>
              </div>
            )}
            {languages.python && (
              <div className="language-item">
                <b>Python</b>
                <div className="progress">
                  <i style={{ width: `${languages.python.percentage}%` }} />
                </div>
                <span>{languages.python.completed} / {languages.python.total}</span>
              </div>
            )}
          </div>
        </article>

        {/* Achievements */}
        <article className="profile-card achievements-card">
          <header className="card-header">
            <h3>Achievements</h3>
          </header>
          <div className="achievements-grid">
            <div className={`achievement ${achievements.firstSolve ? 'unlocked' : 'locked'}`}>
              <span>🎯</span>
              <small>First Solve</small>
            </div>
            <div className={`achievement ${achievements.tenSolves ? 'unlocked' : 'locked'}`}>
              <span>⭐</span>
              <small>10 Solves</small>
            </div>
            <div className={`achievement ${achievements.twentySolves ? 'unlocked' : 'locked'}`}>
              <span>💎</span>
              <small>20 Solves</small>
            </div>
            <div className={`achievement ${achievements.perfectScore ? 'unlocked' : 'locked'}`}>
              <span>💯</span>
              <small>Perfect Score</small>
            </div>
            <div className={`achievement ${achievements.polyglot ? 'unlocked' : 'locked'}`}>
              <span>🌐</span>
              <small>Polyglot</small>
            </div>
          </div>
        </article>

        {/* Security Card */}
        <article className="profile-card security-card">
          <header className="card-header">
            <h3>Security</h3>
          </header>
          {showPwForm ? (
            <form onSubmit={handleChangePassword}>
              <label>
                Current Password
                <input
                  type="password"
                  value={pwForm.oldPassword}
                  onChange={(e) => setPwForm({ ...pwForm, oldPassword: e.target.value })}
                  required
                />
              </label>
              <label>
                New Password
                <input
                  type="password"
                  value={pwForm.newPassword}
                  onChange={(e) => setPwForm({ ...pwForm, newPassword: e.target.value })}
                  required
                />
              </label>
              <label>
                Confirm Password
                <input
                  type="password"
                  value={pwForm.confirmPassword}
                  onChange={(e) => setPwForm({ ...pwForm, confirmPassword: e.target.value })}
                  required
                />
              </label>
              {pwMessage && <p className="form-message">{pwMessage}</p>}
              <div className="button-group">
                <button className="run-button">Update Password</button>
                <button type="button" className="link-button" onClick={() => {
                  setShowPwForm(false)
                  setPwForm({ oldPassword: '', newPassword: '', confirmPassword: '' })
                  setPwMessage('')
                }}>Cancel</button>
              </div>
            </form>
          ) : (
            <button className="run-button full" onClick={() => setShowPwForm(true)}>Change Password</button>
          )}
        </article>
      </div>
    </section>
  )
}

function AdminDashboard({ data, req, reload }) {
  const blankProblem = { language: 'JAVA', difficulty: 'EASY', title: '', topic: '', description: '', starterCode: '' }
  const blankCase = { input: '', expectedOutput: '', hidden: false }
  const [problemForm, setProblemForm] = useState(blankProblem)
  const [caseForm, setCaseForm] = useState(blankCase)
  const [caseEditId, setCaseEditId] = useState(null)
  const [selectedProblem, setSelectedProblem] = useState(null)
  const [message, setMessage] = useState('')
  const [caseBusy, setCaseBusy] = useState(false)

  async function saveProblem(event) {
    event.preventDefault()
    const editing = Boolean(selectedProblem)
    try {
      const savedProblem = await req(editing ? `/admin/problems/${selectedProblem.id}` : '/admin/problems', { method: editing ? 'PUT' : 'POST', body: JSON.stringify(problemForm) })
      if (!editing) {
        const cases = await req(`/admin/problems/${savedProblem.id}/test-cases`)
        setSelectedProblem({ ...savedProblem, cases })
        setProblemForm(savedProblem)
      } else {
        setProblemForm(blankProblem)
        setSelectedProblem(null)
      }
      setMessage('Problem saved.')
      await reload()
    } catch (e) { setMessage(e.message) }
  }
  async function removeProblem(id) {
    try { await req(`/admin/problems/${id}`, { method: 'DELETE' }); setMessage('Problem deleted.'); reload() } catch (e) { setMessage(e.message) }
  }
  async function saveCase(event) {
    event.preventDefault()
    if (!selectedProblem) {
      setMessage('Select a problem first.')
      return
    }
    if (caseBusy) return
    setCaseBusy(true)
    setMessage('')
    const taskId = selectedProblem.id
    try {
      await req(caseEditId ? `/admin/test-cases/${caseEditId}` : `/admin/problems/${taskId}/test-cases`, {
        method: caseEditId ? 'PUT' : 'POST',
        body: JSON.stringify(caseForm)
      })
      const cases = await req(`/admin/problems/${taskId}/test-cases`)
      setSelectedProblem(current => current?.id === taskId ? { ...current, cases } : current)
      setCaseForm(blankCase)
      setCaseEditId(null)
      setMessage('Test case saved.')
      await reload()
    } catch (e) {
      setMessage(e.message || 'Unable to save test case.')
    } finally {
      setCaseBusy(false)
    }
  }
  async function removeCase(id) {
    try { await req(`/admin/test-cases/${id}`, { method: 'DELETE' }); setMessage('Test case deleted.'); reload() } catch (e) { setMessage(e.message) }
  }
  async function toggleCase(testCase) {
    try { await req(`/admin/test-cases/${testCase.id}`, { method: 'PUT', body: JSON.stringify({ ...testCase, hidden: !testCase.hidden }) }); setMessage('Test case visibility updated.'); reload() } catch (e) { setMessage(e.message) }
  }
  async function chooseProblem(problem) {
    setSelectedProblem(problem)
    setProblemForm(problem)
    setCaseEditId(null)
    setCaseForm(blankCase)
    try {
      const cases = await req(`/admin/problems/${problem.id}/test-cases`)
      setSelectedProblem(current => current?.id === problem.id ? { ...current, cases } : current)
    } catch (e) {
      setMessage(e.message || 'Unable to load test cases.')
    }
  }

  const cases = selectedProblem?.cases || []
  return (
    <section className="admin-dashboard">
      <div className="workspace-head"><div><p className="section-kicker">CODEJAR ADMINISTRATION</p><h2>Control center</h2></div><span className="admin-role">ADMIN</span></div>
      {message && <p className="form-message success">{message}</p>}
      <div className="admin-grid">
        <article className="admin-card admin-wide"><header className="card-header"><h3>Problems</h3><span>{data.problems.length} total</span></header>
          <div className="admin-list">{data.problems.map(problem => <div className="admin-row" key={problem.id}><div><b>{problem.title}</b><small>{problem.language} · {problem.topic}</small></div><button className="link-button" onClick={() => chooseProblem(problem)}>Edit</button><button className="danger-button" onClick={() => removeProblem(problem.id)}>Delete</button></div>)}</div>
          <form className="admin-form" onSubmit={saveProblem}><h4>{selectedProblem ? 'Edit problem' : 'Add problem'}</h4><select value={problemForm.language} onChange={e => setProblemForm({ ...problemForm, language: e.target.value })}><option value="JAVA">Java</option><option value="PYTHON">Python</option></select><select value={problemForm.difficulty || 'EASY'} onChange={e => setProblemForm({ ...problemForm, difficulty: e.target.value })}><option value="EASY">Easy</option><option value="HARD">Hard</option></select><input placeholder="Title" value={problemForm.title} onChange={e => setProblemForm({ ...problemForm, title: e.target.value })} required /><input placeholder="Topic" value={problemForm.topic} onChange={e => setProblemForm({ ...problemForm, topic: e.target.value })} required /><textarea placeholder="Description" value={problemForm.description} onChange={e => setProblemForm({ ...problemForm, description: e.target.value })} required /><textarea placeholder="Starter code" value={problemForm.starterCode} onChange={e => setProblemForm({ ...problemForm, starterCode: e.target.value })} required /><button className="run-button">{selectedProblem ? 'Save problem' : 'Add problem'}</button></form>
        </article>
        <article className="admin-card"><header className="card-header"><h3>Test Cases</h3><span>{selectedProblem ? selectedProblem.title : 'Select a problem'}</span></header><div className="admin-list">{cases.map(item => <div className="admin-row" key={item.id}><div><b>{item.hidden ? 'Hidden' : 'Visible'}</b><small>{item.input.replace(/\n/g, ' ')} → {item.expectedOutput}</small></div><button className="link-button" onClick={() => { setCaseEditId(item.id); setCaseForm({ input: item.input, expectedOutput: item.expectedOutput, hidden: item.hidden }) }}>Edit</button><button className="link-button" onClick={() => toggleCase(item)}>{item.hidden ? 'Show' : 'Hide'}</button><button className="danger-button" onClick={() => removeCase(item.id)}>Delete</button></div>)}</div><form className="admin-form" onSubmit={saveCase}><h4>{caseEditId ? 'Edit test case' : 'Add test case'}</h4><textarea placeholder="Input" value={caseForm.input} onChange={e => setCaseForm({ ...caseForm, input: e.target.value })} required /><textarea placeholder="Expected output" value={caseForm.expectedOutput} onChange={e => setCaseForm({ ...caseForm, expectedOutput: e.target.value })} required /><label className="check-label"><input type="checkbox" checked={caseForm.hidden} onChange={e => setCaseForm({ ...caseForm, hidden: e.target.checked })} /> Hidden test case</label><button className="run-button" disabled={!selectedProblem || caseBusy}>{caseBusy ? 'Saving...' : caseEditId ? 'Save test case' : selectedProblem ? 'Add test case' : 'Select a problem first'}</button></form></article>
        <article className="admin-card"><header className="card-header"><h3>Users</h3><span>{data.users.length} registered</span></header><div className="admin-list">{data.users.map(item => <div className="admin-row" key={item.id}><div><b>{item.name}</b><small>{item.email}</small></div><span className="admin-role">{item.role}</span></div>)}</div></article>
        <article className="admin-card admin-wide"><header className="card-header"><h3>Submissions</h3><span>{data.submissions.length} total</span></header><div className="admin-table"><div className="admin-table-head"><span>User</span><span>Problem</span><span>Result</span><span>Score</span><span>Submitted</span></div>{data.submissions.map(item => <div className="admin-table-row" key={item.id}><span>{item.user}</span><span>{item.problem}<small>{item.language}</small></span><span>{item.status.replaceAll('_', ' ') }<small>{item.passedTests}/{item.totalTests} tests</small></span><span>{item.score}%</span><span>{new Date(item.submittedAt).toLocaleString()}</span></div>)}</div></article>
      </div>
    </section>
  )
}

function Workspace({ language, languageMeta, difficulty, setDifficulty, problems, problem, code, setCode, input, setInput, result, resultModal, busy, selectProblem, run, submit, go, setResultModal }) {
  const fileName = languageMeta?.fileName || 'Main.java'
  const icon = languageMeta?.icon || 'JV'

  return (
    <section className="workspace">
      <div className="workspace-head">
        <div>
          <p className="section-kicker">{language} PROBLEM WORKSPACE</p>
          <h2>{problem?.title || 'Choose a problem'}</h2>
        </div>
        <button className="link-button" onClick={() => go('/dashboard')}>← Dashboard</button>
      </div>

      <div className="three-column">
        <aside className="panel problem-sidebar">
          <header className="panel-head"><span>PROBLEMS</span><span className="difficulty-tabs"><button className={difficulty === 'EASY' ? 'active' : ''} onClick={() => setDifficulty('EASY')}>Easy</button><button className={difficulty === 'HARD' ? 'active' : ''} onClick={() => setDifficulty('HARD')}>Hard</button></span></header>
          <p className="problem-count">{difficulty === 'EASY' ? 'Easy' : 'Hard'} challenges ({problems.length})</p>
          {problems.map((x, i) => (
            <button className={'problem-item ' + (x.id === problem?.id ? 'active' : '')} key={x.id} onClick={() => selectProblem(x.id)}>
              <span>{i + 1}. {x.title}</span>
              <i>{x.status === 'ACCEPTED' ? '✓' : x.status === 'PENDING' ? '' : '◷'}</i>
            </button>
          ))}
        </aside>

        <article className="panel code-panel">
          <header className="panel-head">
            <span>{fileName}</span>
            <span className="lang-dot">{icon}</span>
          </header>
          <div className="editor-wrap">
            <div className="line-numbers">
              {code.split('\n').map((_, i) => <span key={i}>{i + 1}</span>)}
            </div>
            <textarea value={code} onChange={e => setCode(e.target.value)} spellCheck="false" />
          </div>
          <footer className="panel-footer">
            <button className="submit-button" onClick={submit} disabled={busy || !problem}>Submit</button>
            <button className="run-button" onClick={run} disabled={busy}>{busy ? 'Running…' : '▶ Run'}</button>
          </footer>
        </article>

        <aside className="side-panels">
          <article className="panel problem-panel">
            <header className="panel-head">Problem details</header>
            <div className="problem-copy">
              <b>{problem?.topic}</b>
              <h3>{problem?.title}</h3>
              <p>{problem?.description || 'Select a problem from the list.'}</p>
              {problem?.examples?.map((e, i) => (
                <pre key={i}>Example {i + 1}{'\n'}Input:{'\n'}{e.input}{'\n'}Output:{'\n'}{e.expectedOutput}</pre>
              ))}
            </div>
          </article>

          <article className="panel input-panel">
            <header className="panel-head">
              <span>Standard input</span>
              <small>run only</small>
            </header>
            <textarea value={input} onChange={e => setInput(e.target.value)} placeholder="Custom input…" />
          </article>
        </aside>
      </div>

      <article className="panel output-panel wide-output">
        <header className="panel-head">
          <span>Console output / test result</span>
          {result.executionTime != null && <small>{result.executionTime} ms</small>}
        </header>
        <pre className={result.error ? 'has-error' : ''}>{result.error || result.output || 'No output yet.'}</pre>
      </article>

      {resultModal && (
        <div className="result-modal-backdrop" onClick={() => setResultModal(null)}>
          <div className="result-modal" onClick={e => e.stopPropagation()}>
            <div className="result-modal-header">
              <span className={`result-modal-icon ${resultModal.type}`}>{resultModal.type === 'success' ? '✅' : resultModal.type === 'warning' ? '⚠' : '❌'}</span>
              <h3>{resultModal.title}</h3>
            </div>
            {resultModal.message && <p className="result-modal-message">{resultModal.message}</p>}
            {resultModal.status !== 'ERROR' && resultModal.totalTests > 0 && (
              <p className="result-modal-stats">{resultModal.passedTests} / {resultModal.totalTests} Test Cases Passed</p>
            )}
            {resultModal.status !== 'ERROR' && resultModal.score != null && (
              <p className="result-modal-score">Score: {resultModal.score}%</p>
            )}
            {resultModal.executionTime != null && resultModal.executionTime > 0 && (
              <p className="result-modal-time">Execution Time: {resultModal.executionTime} ms</p>
            )}
            <button className="run-button full" onClick={() => setResultModal(null)}>Close</button>
          </div>
        </div>
      )}
    </section>
  )
}
