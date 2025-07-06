package lite.sqlite.server.cmdinterface;

import lite.sqlite.server.cmdinterface.domain.commands.QueryData;

public interface IParser {
    public QueryData queryCmd();
    public Object updateCmd();
}