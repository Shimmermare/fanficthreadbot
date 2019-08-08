package fanficthread.fanficthreadbot;

public class AutoRole
{
    private final long roleId;
    private final int reqiredTimeSec;

    public AutoRole(long roleId, int reqiredTimeSec)
    {
        this.roleId = roleId;
        this.reqiredTimeSec = reqiredTimeSec;
    }

    public long getRoleId()
    {
        return roleId;
    }

    public int getReqiredTimeSec()
    {
        return reqiredTimeSec;
    }
}
