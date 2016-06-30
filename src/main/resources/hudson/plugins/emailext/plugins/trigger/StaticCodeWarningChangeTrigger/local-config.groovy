f = namespace("/lib/form")

f.entry(title: _("Build Id"), help: "/plugin/email-ext/help/projectConfig/trigger/StaticCodeWarningChangeTrigger.html") {
  f.textarea(name: "buildId", value: instance?.buildId)
}