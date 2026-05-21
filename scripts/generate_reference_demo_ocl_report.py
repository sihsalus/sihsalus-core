#!/usr/bin/env python3
import csv
import json
import re
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
REFERENCE_BACKEND = ROOT / "reference-sources/openmrs-distro-modules/reference-demo-content/configuration/backend_configuration"
SIHSALUS_BACKEND = ROOT / "reference-sources/sihsalus-content/configuration/backend_configuration"
OUT = ROOT / "reports/reference-demo-ocl-cleanup-report.csv"


FIELDNAMES = [
    "row_type",
    "source_kind",
    "batch",
    "file",
    "concept_uuid",
    "concept_id",
    "concept_name",
    "concept_class",
    "datatype",
    "mappings",
    "matched_in_sihsalus_content",
    "matched_in_sihsalus_v4",
    "match_basis",
    "sihsalus_match",
    "risk",
    "action",
    "backend_dependency",
    "code_reference",
    "notes",
]


HIGH_SIGNAL_BATCHES = {
    "diagnosis_core-demo.csv",
    "findings-core_demo.csv",
    "questions_core-demo.csv",
    "convsets-core_demo.csv",
    "covid-core_demo.csv",
    "drugs_concepts-core_demo.csv",
    "openmrs_BasicLabTests_v25_autoexpand-25.2025-11-19_081008.zip",
    "openmrs_DiagnosesStarterKit_v4_autoexpand-4.2025-07-16_045128.zip",
    "openmrs_CIELAllergySet_v7_autoexpand-7.2025-08-11_004701.zip",
    "openmrs_SOAP_vSOAP_Note_autoexpand-SOAP_Note.2024-07-27_070529.zip",
    "openmrs_CLF_v7_autoexpand-7.2025-08-11_002629.zip",
    "openmrs_CDU_v6_autoexpand-6.2025-09-22_081056.zip",
    "openmrs_Billing_202410302350_autoexpand-202410302350.2024-10-30_204428.zip",
    "openmrs_BasicDrugs_v6_autoexpand-6.2025-09-22_081023.zip",
    "openmrs_DrugDispense_v3_autoexpand-3.2025-08-11_031853.zip",
    "openmrs_StockManagementConcepts_v1_autoexpand-1.2025-09-22_094818.zip",
}


def norm(value):
    return re.sub(r"\s+", " ", (value or "").strip()).casefold()


def concept_name(concept):
    if concept.get("display_name"):
        return concept["display_name"]
    for name in concept.get("names", []):
        if name.get("name_type") == "FULLY_SPECIFIED":
            return name.get("name") or ""
    return concept.get("id") or concept.get("uuid") or ""


def mapping_tokens_from_ocl(concept, mappings_by_from):
    tokens = set()
    code = concept.get("id") or concept.get("external_id") or concept.get("uuid")
    for mapping in mappings_by_from.get(code, []):
        source = mapping.get("to_source_name") or mapping.get("to_source_url") or ""
        target = mapping.get("to_concept_code") or ""
        if source and target:
            tokens.add(f"{source}:{target}")
    return tokens


def read_export(path):
    with zipfile.ZipFile(path) as archive:
        with archive.open("export.json") as handle:
            return json.load(handle)


def read_ocl_concepts(path):
    export = read_export(path)
    mappings_by_from = {}
    for mapping in export.get("mappings", []):
        code = mapping.get("from_concept_code")
        if code:
            mappings_by_from.setdefault(code, []).append(mapping)
    rows = []
    for concept in export.get("concepts", []):
        rows.append(
            {
                "source_kind": "ocl_zip",
                "batch": path.name,
                "file": str(path.relative_to(ROOT)),
                "concept_uuid": concept.get("external_id") or concept.get("uuid") or "",
                "concept_id": concept.get("id") or "",
                "concept_name": concept_name(concept),
                "concept_class": concept.get("concept_class") or "",
                "datatype": concept.get("datatype") or "",
                "mappings": sorted(mapping_tokens_from_ocl(concept, mappings_by_from)),
            }
        )
    return rows


def find_header(row, candidates):
    lowered = {key.casefold(): key for key in row.keys() if key}
    for candidate in candidates:
        if candidate.casefold() in lowered:
            return lowered[candidate.casefold()]
    return None


def read_csv_concepts(path):
    rows = []
    with path.open(newline="", encoding="utf-8-sig") as handle:
        reader = csv.DictReader(handle)
        for source_row in reader:
            uuid_key = find_header(source_row, ["Uuid", "UUID"])
            class_key = find_header(source_row, ["Data class", "Class"])
            datatype_key = find_header(source_row, ["Data type", "Data Type", "Datatype"])
            mapping_key = find_header(source_row, ["Same as mappings", "Mappings"])
            name_key = (
                find_header(source_row, ["Fully specified name:en"])
                or find_header(source_row, ["Fully specified name:es"])
                or find_header(source_row, ["Short name:en"])
            )
            mapping_text = source_row.get(mapping_key, "") if mapping_key else ""
            mappings = [item.strip() for item in mapping_text.split(";") if ":" in item]
            rows.append(
                {
                    "source_kind": "concept_csv",
                    "batch": path.name,
                    "file": str(path.relative_to(ROOT)),
                    "concept_uuid": source_row.get(uuid_key, "") if uuid_key else "",
                    "concept_id": "",
                    "concept_name": source_row.get(name_key, "") if name_key else "",
                    "concept_class": source_row.get(class_key, "") if class_key else "",
                    "datatype": source_row.get(datatype_key, "") if datatype_key else "",
                    "mappings": sorted(set(mappings)),
                }
            )
    return rows


def build_sihsalus_index():
    concepts = []
    for path in sorted((SIHSALUS_BACKEND / "ocl").glob("*.zip")):
        is_v4 = "SIHSALUS-v4" in path.name
        for row in read_ocl_concepts(path):
            row["is_v4"] = is_v4
            concepts.append(row)

    by_uuid = {}
    by_id = {}
    by_name = {}
    by_mapping = {}
    for concept in concepts:
        label = f"{concept['batch']}::{concept['concept_id']}::{concept['concept_name']}"
        for value in {concept["concept_uuid"], concept["concept_id"]}:
            if value:
                by_uuid.setdefault(value, []).append((concept, label))
                by_id.setdefault(value, []).append((concept, label))
        if concept["concept_name"]:
            by_name.setdefault(norm(concept["concept_name"]), []).append((concept, label))
        for token in concept["mappings"]:
            by_mapping.setdefault(token, []).append((concept, label))
    return by_uuid, by_id, by_name, by_mapping


def match_reference(row, indexes):
    by_uuid, by_id, by_name, by_mapping = indexes
    matches = []
    basis = []
    if row["concept_uuid"] in by_uuid:
        matches.extend(by_uuid[row["concept_uuid"]])
        basis.append("uuid")
    if row["concept_id"] in by_id:
        matches.extend(by_id[row["concept_id"]])
        basis.append("id")
    for token in row["mappings"]:
        if token in by_mapping:
            matches.extend(by_mapping[token])
            basis.append(f"mapping:{token}")
    name_key = norm(row["concept_name"])
    if name_key in by_name:
        matches.extend(by_name[name_key])
        basis.append("name")

    seen = set()
    unique = []
    for concept, label in matches:
        key = (concept["batch"], concept["concept_id"], concept["concept_uuid"])
        if key not in seen:
            seen.add(key)
            unique.append((concept, label))

    matched_v4 = any(concept.get("is_v4") for concept, _ in unique)
    return unique, sorted(set(basis)), matched_v4


def classify(row, matched, matched_v4):
    batch = row["batch"]
    mappings = " ".join(row["mappings"])
    name = row["concept_name"]
    if matched_v4:
        return (
            "high",
            "review_replace_with_sihsalus_v4",
            "Ya hay match en SIHSALUS-v4; importar reference demo puede duplicar o pisar semántica propia.",
        )
    if matched:
        return (
            "medium",
            "review_existing_sihsalus_content",
            "Existe match en otro paquete SIHSALUS local; validar si debe apuntar al concepto propio.",
        )
    if batch in HIGH_SIGNAL_BATCHES or "org.openmrs.module.emrapi:" in mappings:
        return (
            "medium",
            "import_only_if_backend_or_forms_need_it",
            "Batch clínico o usado por runtime; si se importa, documentar ownership y mappings.",
        )
    if any(word in batch.casefold() for word in ["demo", "soap", "ipd", "covid", "mental"]):
        return (
            "medium",
            "avoid_or_isolate_demo_content",
            "Contenido demo; meterlo al OCL productivo ensucia búsquedas y sets clínicos.",
        )
    if any(word in name.casefold() for word in ["demo", "test", "cookbook"]):
        return (
            "low",
            "avoid_demo_concept",
            "Nombre sugiere contenido de ejemplo.",
        )
    return (
        "low",
        "safe_to_import_after_owner_review",
        "No se detectó match local, pero igual requiere revisión terminológica.",
    )


def backend_dependency_rows():
    rows = []
    emrapi_codes = [
        "Diagnosis Concept Set",
        "Coded Diagnosis",
        "Non-Coded Diagnosis",
        "Diagnosis Order",
        "Primary",
        "Secondary",
        "Diagnosis Certainty",
        "Confirmed",
        "Presumed",
        "Disposition Concept Set",
        "Disposition",
        "Admission Location",
        "Internal Transfer Location",
        "Date of Death",
        "Unknown Cause of Death",
        "Patient Died",
        "Admission Decision",
        "Deny Admission",
        "Consult Free Text Comments",
    ]
    for code in emrapi_codes:
        rows.append(
            dep(
                "emrapi",
                "",
                code,
                "sihsalus-module-emrapi/src/main/java/org/openmrs/module/emrapi/EmrApiConstants.java",
                "required_mapping",
                "must_exist_as_sihsalus_mapping",
                "Backend busca conceptos por mapping source org.openmrs.module.emrapi.",
                mapping=f"org.openmrs.module.emrapi:{code}",
            )
        )

    allergy_defaults = {
        "allergy.concept.severity.mild": "1498AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
        "allergy.concept.severity.moderate": "1499AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
        "allergy.concept.severity.severe": "1500AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
        "allergy.concept.allergen.food": "162553AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
        "allergy.concept.allergen.drug": "162552AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
        "allergy.concept.allergen.environment": "162554AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
        "allergy.concept.reactions": "162555AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
        "allergy.concept.otherNonCoded": "5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
        "allergy.concept.unknown": "1067AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
    }
    for prop, uuid in allergy_defaults.items():
        rows.append(
            dep(
                "openmrs-core-allergy",
                uuid,
                prop,
                "sihsalus-core-api/src/main/java/org/openmrs/util/OpenmrsConstants.java",
                "default_ciel_uuid",
                "override_gp_to_sihsalus_uuid_or_keep_ciel",
                "Core usa global property; si no configuras SIHSALUS, cae al UUID CIEL default.",
            )
        )

    for prop in [
        "order.drugRoutesConceptUuid",
        "order.drugDosingUnitsConceptUuid",
        "order.drugDispensingUnitsConceptUuid",
        "order.durationUnitsConceptUuid",
        "order.testSpecimenSourcesConceptUuid",
        "drugOrder.drugOther",
    ]:
        rows.append(
            dep(
                "openmrs-core-orders",
                "",
                prop,
                "sihsalus-core-api/src/main/java/org/openmrs/api/impl/OrderServiceImpl.java",
                "global_property_concept_set",
                "set_gp_to_sihsalus_concept_set",
                "Validaciones de ordenes y medicamentos dependen de estos concept sets.",
            )
        )

    for uuid, name in {
        "42ed45fd-f3f6-44b6-bfc2-8bde1bb41e00": "ATT DEFAULT ATTACHMENT",
        "7cac8397-53cd-4f00-a6fe-028e8d743f8e": "ATT IMAGE ATTACHMENT",
    }.items():
        rows.append(
            dep(
                "attachments",
                uuid,
                name,
                "sihsalus-module-attachments/src/main/java/org/openmrs/module/attachments/AttachmentsConstants.java",
                "module_bootstrap_concept",
                "keep_module_owned_not_clinical_ocl",
                "Concepto técnico para obs complex; no debería mezclarse con conceptos clínicos propios.",
            )
        )

    rows.append(
        dep(
            "sihsalusinterop",
            "159947",
            "Visit Diagnoses",
            "sihsalus-module-sihsalusinterop/src/main/java/org/openmrs/module/sihsalusinterop/api/mapper/FhirToOpenMRSConditionMapper.java",
            "hardcoded_numeric_id",
            "replace_code_with_uuid_or_mapping",
            "ID numérico local; es incompatible con OCL limpio y bases nuevas.",
        )
    )
    return rows


def dep(module, identifier, name, code_reference, risk, action, notes, mapping=""):
    return {
        "row_type": "backend_dependency",
        "source_kind": module,
        "batch": "",
        "file": "",
        "concept_uuid": identifier if identifier and not identifier.isdigit() else "",
        "concept_id": identifier if identifier.isdigit() else "",
        "concept_name": name,
        "concept_class": "",
        "datatype": "",
        "mappings": mapping,
        "matched_in_sihsalus_content": "",
        "matched_in_sihsalus_v4": "",
        "match_basis": "",
        "sihsalus_match": "",
        "risk": risk,
        "action": action,
        "backend_dependency": module,
        "code_reference": code_reference,
        "notes": notes,
    }


def main():
    indexes = build_sihsalus_index()
    reference_rows = []
    for path in sorted((REFERENCE_BACKEND / "concepts").glob("*.csv")):
        reference_rows.extend(read_csv_concepts(path))
    for path in sorted((REFERENCE_BACKEND / "ocl").glob("*.zip")):
        reference_rows.extend(read_ocl_concepts(path))

    OUT.parent.mkdir(parents=True, exist_ok=True)
    with OUT.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=FIELDNAMES)
        writer.writeheader()
        for row in reference_rows:
            matches, basis, matched_v4 = match_reference(row, indexes)
            risk, action, notes = classify(row, matches, matched_v4)
            writer.writerow(
                {
                    "row_type": "reference_concept",
                    "source_kind": row["source_kind"],
                    "batch": row["batch"],
                    "file": row["file"],
                    "concept_uuid": row["concept_uuid"],
                    "concept_id": row["concept_id"],
                    "concept_name": row["concept_name"],
                    "concept_class": row["concept_class"],
                    "datatype": row["datatype"],
                    "mappings": ";".join(row["mappings"]),
                    "matched_in_sihsalus_content": "yes" if matches else "no",
                    "matched_in_sihsalus_v4": "yes" if matched_v4 else "no",
                    "match_basis": ";".join(basis),
                    "sihsalus_match": " | ".join(label for _, label in matches[:5]),
                    "risk": risk,
                    "action": action,
                    "backend_dependency": "",
                    "code_reference": "",
                    "notes": notes,
                }
            )
        for row in backend_dependency_rows():
            writer.writerow(row)

    print(f"Wrote {OUT.relative_to(ROOT)}")
    print(f"reference_concepts={len(reference_rows)}")
    print(f"backend_dependencies={len(backend_dependency_rows())}")


if __name__ == "__main__":
    main()
