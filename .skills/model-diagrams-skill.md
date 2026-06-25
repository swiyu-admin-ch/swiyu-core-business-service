---
name: domain-diagram-maintenance
description: >
  Guides maintenance of the hand-curated PlantUML domain and component diagrams under
  docs/models/. Activates automatically when .puml files are mentioned, when domain
  entities/enums/relationships change, or when asked to update/review architecture diagrams.
---

# Skill: Domain Diagram Maintenance (PlantUML)

## Context

The `docs/models/` folder contains PlantUML diagrams documenting domain entities and module structure of
the `swiyu-core-business-service`.

## Diagrams & Source Locations

| File                                    | Type              | Description                                                    | Java source                                                                                                   |
|-----------------------------------------|-------------------|----------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| `core-business-domain-model.puml`       | Class diagram     | Management, Status, Identifier, Documents, Trust entities      | `swiyu-core-business-service-app/src/main/java/ch/admin/bj/swiyu/core/business/modules/`                      |
| `registry-status-domain-model.puml`     | Class diagram     | StatusListDatastoreEntity, VcEntity                            | `swiyu-core-business-service-registry-status/src/main/java/ch/admin/bj/swiyu/registry/status/domain/`         |
| `registry-identifier-domain-model.puml` | Class diagram     | IdentifierDatastoreEntity, DidEntity                           | `swiyu-core-business-service-registry-identifier/src/main/java/ch/admin/bj/swiyu/registry/identifier/domain/` |
| `core-business-modules.puml`            | Component diagram | Inter-module dependencies between all modules under `modules/` | `swiyu-core-business-service-app/src/main/java/ch/admin/bj/swiyu/core/business/modules/`                      |

## Viewing

Open `.puml` files in IntelliJ IDEA with the **PlantUML Integration** plugin. All diagrams use
`!pragma layout smetana` — no Graphviz installation needed.

## PlantUML Conventions

When updating a `.puml` file, follow these rules:

- **Entities**: use `entity Name <<entity>>` syntax
- **Enums**: place inside the owning module's package; connect with `Entity --> EnumType : fieldName`
- **Embedded value objects** (e.g. `AuditMetadata`, `Address`): list under `-- embedded --` in the entity body; define
  in the `Common Value Objects` package in `core-business.puml`
- **JSONB fields**: list under `-- jsonb --` separator
- **Cross-module FK relationships**: use ERD notation — `A ||--o{ B : "fkFieldName"`

## Package Color Codes

These colors are used consistently across all diagrams (both class and component diagrams):

| Package / Module     | Color                  |
|----------------------|------------------------|
| Management           | `#E8F5E9` (green)      |
| Status               | `#FFF3E0` (orange)     |
| Identifier           | `#F3E5F5` (purple)     |
| Documents            | `#FCE4EC` (pink)       |
| Trust                | `#E0F7FA` (cyan)       |
| DataImport           | `#F5F5F5` (light grey) |
| Registry Status      | `#EFEBE9` (brown)      |
| Registry Identifier  | `#F9FBE7` (lime)       |
| Common Value Objects | `#EEEEFF` (blue)       |

## Checklist When Updating

### Class diagrams (`core-business.puml`, `registry-*.puml`)

When a Java entity changes:

- [ ] New entity → add to correct package in the relevant `.puml`
- [ ] New enum → add inside owning module package with values; add `-->` line from entity
- [ ] New field → add to entity body
- [ ] New FK relationship → add to the `CROSS-MODULE RELATIONSHIPS` section
- [ ] New embedded value object → add to `Common Value Objects` package in `core-business.puml`
- [ ] Enum values changed → update the enum block in the diagram

### Component diagram (`module-dependencies.puml`)

When modules or their inter-dependencies change:

- [ ] New module added under `modules/` → add a `component "ModuleName" #COLOR as alias` line using the color table
  above
- [ ] New cross-module `import` introduced → run the dependency-discovery command below and add the corresponding `-->`
  arrow
- [ ] Circular / weak dependency (e.g. demo-data only) → use `..>` with a `"label"` note
- [ ] Module removed → remove its component and all arrows referencing it

#### Discovering cross-module dependencies

Run this from the repo root to regenerate the full dependency list:

```bash
BASE=swiyu-core-business-service-app/src/main/java/ch/admin/bj/swiyu/core/business/modules
for src in management status identifier documents trust dataimport; do
  echo "=== $src ==="
  grep -rh "import ch.admin.bj.swiyu.core.business.modules\." "$BASE/$src" 2>/dev/null \
    | grep -oP "modules\.\K[a-z]+" \
    | grep -v "^${src}$" \
    | sort -u
done
```

Each line `X --> Y` in the output means module `X` depends on module `Y`.

