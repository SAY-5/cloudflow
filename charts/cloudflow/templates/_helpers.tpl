{{/* Common labels for every CloudFlow object. */}}
{{- define "cloudflow.labels" -}}
app.kubernetes.io/part-of: cloudflow
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
{{- end -}}

{{/* Selector labels for a named service. */}}
{{- define "cloudflow.selectorLabels" -}}
app: {{ .name }}
app.kubernetes.io/name: {{ .name }}
app.kubernetes.io/instance: {{ .release }}
{{- end -}}

{{/* Fully-qualified image reference for a service block. */}}
{{- define "cloudflow.image" -}}
{{- printf "%s/%s:%s" .global.imageRegistry .svc.image .svc.tag -}}
{{- end -}}
