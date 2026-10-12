from alembic import op
import sqlalchemy as sa


revision = "0003_admin_moderation"
down_revision = "0002_manual_report_location"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.add_column("users", sa.Column("role", sa.String(length=20), server_default="citizen", nullable=False))
    op.alter_column("users", "role", server_default=None)
    op.add_column("reports", sa.Column("moderated_by_id", sa.Uuid(), nullable=True))
    op.add_column("reports", sa.Column("moderated_at", sa.DateTime(timezone=True), nullable=True))
    op.create_foreign_key(
        "fk_reports_moderated_by_id_users",
        "reports",
        "users",
        ["moderated_by_id"],
        ["id"],
        ondelete="SET NULL",
    )


def downgrade() -> None:
    op.drop_constraint("fk_reports_moderated_by_id_users", "reports", type_="foreignkey")
    op.drop_column("reports", "moderated_at")
    op.drop_column("reports", "moderated_by_id")
    op.drop_column("users", "role")
