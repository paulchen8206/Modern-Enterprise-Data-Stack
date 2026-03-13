# 🧊 Apache Iceberg Tables Guide

This project supports writing Spark batch output to Apache Iceberg tables.

## <span style="color: #0ea5e9;">Current Architecture Context</span>

- Spark implementation lives in `pipelines/spark/` and image definitions are in `infra/dockerfiles/`.
- Iceberg batch writes are validated first in Compose-based local runtime (`make up`) and can be smoke-tested in Kind after `make kind-deploy`.
- CI/CD orchestration for changes is split between `.github/workflows/ci.yml` and `.github/workflows/cd.yml`.
- Branch and environment flow follows `dev` push (CI/dev checks) and PR promotion to `qa`/`stg`/`prd` (env CI checks plus Helm CD deployment).

## <span style="color: #0ea5e9;">What Was Added</span>

- Iceberg runtime JAR in Spark image:
  - `infra/dockerfiles/spark.Dockerfile`
- Optional Iceberg write path in batch job:
  - `pipelines/spark/spark_batch_job.py`
- Make target for Iceberg demo run:
  - `make run-iceberg-demo`

## <span style="color: #0ea5e9;">Default Iceberg Settings</span>

The batch job reads these environment variables:

- `ENABLE_ICEBERG` (default: `false`)
- `ICEBERG_CATALOG` (default: `local`)
- `ICEBERG_NAMESPACE` (default: `analytics`)
- `ICEBERG_TABLE` (default: `orders`)
- `ICEBERG_WAREHOUSE` (default: `file:///tmp/iceberg_warehouse`)

## <span style="color: #0ea5e9;">Run Iceberg Demo</span>

1. Rebuild/start the stack so Spark has the Iceberg JAR:

```bash
make up
```

2. Run batch job with Iceberg enabled:

```bash
make run-iceberg-demo
```

The job still writes CSV outputs as before, and additionally writes to Iceberg table:

- `local.analytics.orders`

## <span style="color: #0ea5e9;">Production Procedure</span>

1. Validate local run with `make run-iceberg-demo` and inspect Spark logs.
2. Choose warehouse path strategy (`local` file path or object storage path).
3. Define namespace/table naming convention by domain/environment.
4. Validate read queries from downstream engines before rollout.
5. Promote configuration to higher environments with the same table contract.

## <span style="color: #0ea5e9;">Best Practices</span>

- Use partitioning aligned to dominant query filters.
- Keep schema evolution backward-compatible; avoid destructive column changes.
- Use snapshot retention and compaction maintenance to control storage costs.
- Separate dev/test/prod namespaces to avoid accidental cross-environment reads.
- Capture table metadata checks in CI to prevent accidental contract breakage.

## <span style="color: #0ea5e9;">Validation Checklist</span>

- Confirm demo command exits successfully:

  ```bash
  make run-iceberg-demo
  ```

- Confirm logs include:
  - `Iceberg write complete: local.analytics.orders`
  - `Batch ETL process completed successfully`

- Confirm Spark image includes required jars:
  - Iceberg runtime jar
  - Hadoop AWS + AWS SDK bundle jars

## <span style="color: #0ea5e9;">Run Without Iceberg</span>

To use legacy behavior (CSV outputs only):

```bash
make run-batch-job
```

## <span style="color: #0ea5e9;">Notes</span>

- Iceberg is implemented with Spark Catalog type `hadoop` and a local warehouse by default.
- You can switch to object storage-backed warehouse paths by setting `ICEBERG_WAREHOUSE` and related Spark/Hadoop settings for your environment.

## <span style="color: #0ea5e9;">Troubleshooting</span>

- `spark-submit` not found:
  - Use explicit binary path `/opt/spark/bin/spark-submit` in container commands.
- `S3AFileSystem not found`:
  - Ensure Spark image includes `hadoop-aws` and `aws-java-sdk-bundle` jars.
- MinIO bucket not found:
  - Ensure `raw-data` and `processed-data` buckets exist before running batch jobs.
