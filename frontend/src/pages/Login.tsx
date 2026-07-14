import { useSearchParams } from 'react-router-dom';

export default function Login() {
  const [params] = useSearchParams();
  const error = params.get('error');

  return (
    <main style={{ display: 'flex', flexDirection: 'column', minHeight: '90dvh' }}>
      <div
        style={{
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center'
        }}
      >
        <p className="eyebrow">Finsight</p>

        <h1
          style={{
            fontFamily: 'var(--display)',
            fontSize: 40,
            lineHeight: 1.05,
            fontWeight: 800,
            letterSpacing: '-0.03em',
            marginTop: 12
          }}
        >
          Sabes con qué
          <br />
          pagar hoy.
        </h1>

        <p style={{ color: 'var(--muted)', marginTop: 14, fontSize: 16, lineHeight: 1.5 }}>
          Tus tarjetas, tus cuentas y tus deudas en una sola pantalla. Registra un gasto
          en un mensaje, no en cinco pantallas.
        </p>

        {error && (
          <div
            className="card"
            style={{
              marginTop: 22,
              borderColor: 'var(--danger)',
              background: 'var(--danger-soft)',
              color: 'var(--danger)',
              fontSize: 14
            }}
          >
            {error}
          </div>
        )}
      </div>

      {/* Navegación completa, no fetch: el OAuth necesita al navegador. */}
      <a href="/oauth2/authorization/google" style={{ textDecoration: 'none' }}>
        <button className="btn">Entrar con Google</button>
      </a>

      <p
        style={{
          color: 'var(--muted)',
          fontSize: 13,
          textAlign: 'center',
          marginTop: 14
        }}
      >
        Al entrar aceptas que Finsight guarde tus datos financieros.
      </p>
    </main>
  );
}