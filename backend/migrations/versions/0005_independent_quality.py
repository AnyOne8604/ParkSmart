"""Persist classifier suggestions before human review for independent evaluation."""

from alembic import op
import sqlalchemy as sa

revision = "0005_independent_quality"
down_revision = "0004_analytics_classification"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.add_column("reports", sa.Column("suggested_category", sa.String(length=40), nullable=True))
    op.add_column("reports", sa.Column("suggestion_confidence", sa.Float(), nullable=True))
    op.add_column("reports", sa.Column("classifier_version", sa.String(length=40), nullable=True))
    op.add_column("reports", sa.Column("suggestion_created_at", sa.DateTime(timezone=True), nullable=True))


def downgrade() -> None:
    op.drop_column("reports", "suggestion_created_at")
    op.drop_column("reports", "classifier_version")
    op.drop_column("reports", "suggestion_confidence")
    op.drop_column("reports", "suggested_category")
