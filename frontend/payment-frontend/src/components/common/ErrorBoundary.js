import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Component } from "react";
export class ErrorBoundary extends Component {
    constructor() {
        super(...arguments);
        this.state = { hasError: false, error: null };
    }
    static getDerivedStateFromError(error) {
        return { hasError: true, error };
    }
    componentDidCatch(error, info) {
        console.error("ErrorBoundary caught:", error, info);
    }
    render() {
        if (this.state.hasError) {
            if (this.props.fallback)
                return this.props.fallback;
            return (_jsx("div", { className: "error-boundary", children: _jsxs("div", { className: "result-card result-card--error", children: [_jsx("div", { className: "result-card__icon", children: "\u26A0" }), _jsx("h2", { children: "Something went wrong" }), _jsx("p", { children: this.state.error?.message }), _jsx("button", { className: "btn btn--primary", onClick: () => this.setState({ hasError: false, error: null }), children: "Try again" })] }) }));
        }
        return this.props.children;
    }
}
