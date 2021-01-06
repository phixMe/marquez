package marquez.db.mappers;

import lombok.NonNull;
import marquez.db.Columns;
import marquez.db.models.LineageDatasetRow;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

import static marquez.db.Columns.stringOrNull;
import static marquez.db.Columns.stringOrThrow;
import static marquez.db.Columns.timestampOrNull;
import static marquez.db.Columns.timestampOrThrow;
import static marquez.db.Columns.uuidArrayOrThrow;
import static marquez.db.Columns.uuidOrNull;
import static marquez.db.Columns.uuidOrThrow;

public final class LineageDatasetRowMapper implements RowMapper<LineageDatasetRow> {
    @Override
    public LineageDatasetRow map(@NonNull ResultSet results, @NonNull StatementContext context)
            throws SQLException {
        return new LineageDatasetRow(
                uuidOrThrow(results, Columns.ROW_UUID),
                stringOrThrow(results, Columns.TYPE),
                timestampOrThrow(results, Columns.CREATED_AT),
                timestampOrThrow(results, Columns.UPDATED_AT),
                uuidOrThrow(results, Columns.NAMESPACE_UUID),
                stringOrThrow(results, Columns.NAMESPACE_NAME),
                uuidOrThrow(results, Columns.SOURCE_UUID),
                stringOrThrow(results, Columns.SOURCE_NAME),
                stringOrThrow(results, Columns.NAME),
                stringOrThrow(results, Columns.PHYSICAL_NAME),
                uuidArrayOrThrow(results, Columns.TAG_UUIDS),
                timestampOrNull(results, Columns.LAST_MODIFIED_AT),
                stringOrNull(results, Columns.DESCRIPTION),
                uuidOrNull(results, Columns.CURRENT_VERSION_UUID),
                stringOrNull(results, Columns.IO_TYPE));
    }
}
