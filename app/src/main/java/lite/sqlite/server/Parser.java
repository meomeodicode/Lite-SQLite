package lite.sqlite.server;

import lite.sqlite.server.model.domain.commands.QueryData;

public interface Parser {
    public QueryData queryCmd();
    public Object updateCmd();
}