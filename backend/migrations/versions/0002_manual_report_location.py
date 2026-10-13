# Migración v0.3.0: incorpora localidad/dirección y permite que el GPS sea opcional para reportes manuales.
from alembic import op
import sqlalchemy as sa

revision = "0002_manual_report_location"
down_revision = "0001_initial"
branch_labels = None
depends_on = None


# Añade los datos textuales de ubicación y ajusta la geometría para admitir NULL.
def upgrade() -> None:
    op.add_column(
        "reports",
        sa.Column("locality", sa.String(length=40), nullable=False, server_default="Sin especificar"),
    )
    op.add_column(
        "reports",
        sa.Column("address", sa.String(length=250), nullable=False, server_default="Dirección no registrada en v0.2"),
    )
    op.alter_column("reports", "locality", server_default=None)
    op.alter_column("reports", "address", server_default=None)
    op.alter_column("reports", "location", nullable=True)


# Quita los campos agregados; mantiene location nullable para no invalidar reportes manuales existentes.
def downgrade() -> None:
    op.drop_column("reports", "address")
    op.drop_column("reports", "locality")
    # Keep location nullable so downgrading never destroys manual-only report data.
