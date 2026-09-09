A price is alway positive
A price can have up to 2 decimal places, but can also have no decimal, when it is an integer
If a price has more than 2 decimal places, it should be rounded up if over .5 and not an error
A price is always in EURO
A currency change is not handled by the pricing domain
A price over 100000 is not allowed
A price always has a taxable amount - the tax is a percentage. The price can be represented without tax, or with tax, and the distiction is alway clear.
The tax rate must be between 0% and 100% inclusive. A price with tax is rounded to 2 decimal places, the same as a price
without tax. Because of this rounding, two tax rates that are very close to each other are not guaranteed to produce
strictly ordered tax-inclusive amounts (e.g. two rates a hundredth of a percent apart may round to the same amount, or
appear to invert by less than a cent) — only rates that differ enough to matter at the cent level are guaranteed to
order correctly.
