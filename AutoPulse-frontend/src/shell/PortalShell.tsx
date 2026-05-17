import {NavLink, Outlet, useLocation} from "react-router-dom";
import {useState} from "react";
import {useAuth} from "@/features/auth/AuthContext";
import {navItems} from "@/shell/navigation";
import {ThemeToggleButton} from "@/components/ThemeToggleButton";

export function PortalShell() {
    const {user, logout} = useAuth();
    const location = useLocation();
    const [isLoggingOut, setIsLoggingOut] = useState(false);
    const [activeTabRefreshVersion, setActiveTabRefreshVersion] = useState(0);
    const visibleNavItems = navItems.filter((item) => (user ? item.roles.some((role) => user.roles.includes(role)) : false));
    const quickNavItems = visibleNavItems.slice(0, 4);

    async function handleLogout() {
        setIsLoggingOut(true);
        await logout();
    }

    function handleNavSelect(path: string) {
        if (location.pathname === path) {
            setActiveTabRefreshVersion((current) => current + 1);
        }
    }

    return (
        <div className="portal-layout">
            <header className="topbar">
                <div className="topbar-main">
                    <h1 className="topbar-brand">AutoPulse Operations</h1>
                    <nav className="top-nav" aria-label="Quick navigation">
                        {quickNavItems.map((item) => (
                            <NavLink
                                key={`top-${item.path}`}
                                to={item.path}
                                onClick={() => handleNavSelect(item.path)}
                                className={({isActive}) => (isActive ? "top-nav-item active" : "top-nav-item")}
                            >
                                {item.label}
                            </NavLink>
                        ))}
                    </nav>
                </div>
                <div className="topbar-actions">
                    <span className="system-pill">
                        <span className="pulse-dot" aria-hidden="true"/>
                        API CONNECTED
                    </span>
                    <ThemeToggleButton/>
                    <span className="role-pill">{user?.roles.length ? user.roles.join(", ") : "NO ROLE"}</span>
                    <div className="topbar-user-block">
                        <p className="topbar-label">Signed in as</p>
                        <p className="topbar-user">{user?.fullName}</p>
                    </div>
                    <button className="secondary-btn" type="button" onClick={handleLogout} disabled={isLoggingOut}>
                        {isLoggingOut ? "Logging out..." : "Logout"}
                    </button>
                </div>
            </header>

            <div className="portal-body">
                <aside className="portal-sidebar">
                    <div className="brand-block">
                        <p className="brand-eyebrow">Command Center</p>
                        <h2>Operations Portal</h2>
                        <p className="brand-status">
                            <span className="pulse-dot" aria-hidden="true"/>
                            Active modules: {visibleNavItems.length}
                        </p>
                    </div>

                    <nav className="main-nav" aria-label="Main navigation">
                        {visibleNavItems.map((item) => (
                            <NavLink
                                key={item.path}
                                to={item.path}
                                onClick={() => handleNavSelect(item.path)}
                                className={({isActive}) => (isActive ? "nav-item active" : "nav-item")}
                            >
                                <strong>{item.label}</strong>
                                <span>{item.description}</span>
                            </NavLink>
                        ))}
                    </nav>

                    <div className="sidebar-footer">
                        <span>AutoPulse WebUI Version 1.0</span>
                    </div>
                </aside>

                <div className="portal-main">
                    <main className="content-area">
                        <Outlet key={`${location.pathname}:${activeTabRefreshVersion}`}/>
                    </main>
                </div>
            </div>
        </div>
    );
}
