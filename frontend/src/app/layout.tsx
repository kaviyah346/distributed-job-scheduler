import type { Metadata } from 'next';
import '../styles/globals.css';
import { ToastProvider } from '@/components/Toast';
import { Navbar } from '@/components/Navbar';
import { Sidebar } from '@/components/Sidebar';

export const metadata: Metadata = {
  title: 'Distributed Job Scheduler Platform',
  description: 'Production-inspired Distributed Job Scheduler Console with Keycloak OIDC, PostgreSQL SKIP LOCKED, and Real-time Worker Metrics',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className="dark">
      <body className="bg-background text-foreground antialiased min-h-screen flex flex-col">
        <ToastProvider>
          <Navbar />
          <div className="flex flex-1">
            <Sidebar />
            <main className="flex-1 p-6 overflow-y-auto max-w-7xl">
              {children}
            </main>
          </div>
        </ToastProvider>
      </body>
    </html>
  );
}
