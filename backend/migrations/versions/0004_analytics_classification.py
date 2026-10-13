"""Add human-reviewed category labels for assisted classification."""

# Migración v0.6.0: conserva la clasificación confirmada por personas para analítica y entrenamiento local.
from alembic import op
import sqlalchemy as sa

revision = "0004_analytics_classification"
down_revision = "0003_admin_moderation"
branch_labels = None
depends_on = None


# Agrega etiqueta revisada y fecha de revisión; el índice acelera consultas de muestras etiquetadas.
def upgrade() -> None:
    op.add_column("reports", sa.Column("reviewed_category", sa.String(length=40), nullable=True))
    op.add_column("reports", sa.Column("category_reviewed_at", sa.DateTime(timezone=True), nullable=True))
    op.create_index("ix_reports_category_reviewed_at", "reports", ["category_reviewed_at"])


# Retira índice y etiquetas humanas incorporadas por esta revisión.
def downgrade() -> None:
    op.drop_index("ix_reports_category_reviewed_at", table_name="reports")
    op.drop_column("reports", "category_reviewed_at")
    op.drop_column("reports", "reviewed_category")
