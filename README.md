# pdfunpassword

Sometimes, you get PDFs with passwords on them. Probably from your CPA, and the
password is probably an SSN or EIN. You keep those in 1Password, but generally
speaking the password on the PDF is mostly an annoyance for everyone, so you
want to remove it (or at least make it easier to open). At the same time you
want to not memorize EINs/SSNs, or have them show up in shell history. Do you
believe EINs and SSNs have dashes in them but your CPA doesn't? We got you.

This does all of that with [babashka]. It assumes you have 1Password installed
an operational, including the `op` CLI, as well as qpdf (or at least homebrew so
it can install qpdf).

[babashka]: https://babashka.org/

