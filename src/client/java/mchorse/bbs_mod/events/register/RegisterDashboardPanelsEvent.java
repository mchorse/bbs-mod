package mchorse.bbs_mod.events.register;

import mchorse.bbs_mod.ui.dashboard.UIDashboard;

public class RegisterDashboardPanelsEvent
{
    public final UIDashboard dashboard;

    public RegisterDashboardPanelsEvent(UIDashboard dashboard)
    {
        this.dashboard = dashboard;
    }
}
