"""SMTP capture proof; never delivery to an external mailbox."""
from email.message import EmailMessage
import json
import smtplib
import time
import urllib.request
import uuid


def probe_mail(env):
    host, port = env["MAIL_TEST_SMTP"].split(":")
    marker = "synthetic-" + uuid.uuid4().hex
    message = EmailMessage()
    message["From"] = "fixture@example.test"
    message["To"] = "recipient@example.test"
    message["Subject"] = marker
    message.set_content("Synthetic fixture message; no account credentials.")
    with smtplib.SMTP(host, int(port), timeout=5) as smtp:
        smtp.send_message(message)
        try:
            smtp.sendmail("fixture@example.test", ["reject@example.invalid"], message.as_bytes())
        except smtplib.SMTPRecipientsRefused:
            pass
        else:
            raise RuntimeError("Mail sandbox accepted a non-test recipient")
    http = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    for _ in range(30):
        with http.open(env["MAIL_TEST_HTTP"] + "/api/v1/messages", timeout=5) as response:
            data = json.load(response)
        matched = [item for item in data["messages"] if item["Subject"] == marker]
        if matched:
            with http.open(env["MAIL_TEST_HTTP"] + "/api/v1/message/" + matched[0]["ID"], timeout=5) as response:
                body = json.load(response)
            if "Synthetic fixture message" not in body["Text"]:
                raise RuntimeError("Captured SMTP body differs")
            print("Synthetic SMTP capture and non-test recipient rejection: PASS", flush=True)
            return
        time.sleep(0.1)
    raise RuntimeError("Synthetic SMTP message was not captured")
