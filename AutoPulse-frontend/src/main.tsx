import React from "react";
import ReactDOM from "react-dom/client";
import {RouterProvider} from "react-router-dom";
import {router} from "@/router";
import {AuthProvider} from "@/features/auth/AuthContext";
import {OpsDataProvider} from "@/features/ops/OpsDataContext";
import {ThemeProvider} from "@/features/common/theme/ThemeContext";
import "@/styles/global.css";

ReactDOM.createRoot(document.getElementById("root")!).render(
    <React.StrictMode>
        <ThemeProvider>
            <AuthProvider>
                <OpsDataProvider>
                    <RouterProvider router={router}/>
                </OpsDataProvider>
            </AuthProvider>
        </ThemeProvider>
    </React.StrictMode>
);
