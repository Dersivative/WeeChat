import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { AppShell } from '../widgets/app'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<AppShell />} />
        <Route path="*" element={<AppShell />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
