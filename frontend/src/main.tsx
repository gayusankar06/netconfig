import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.tsx'
import { initKeycloak } from './keycloak'

const renderApp = () => {
  ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
      <App />
    </React.StrictMode>,
  )
}

initKeycloak(renderApp);
