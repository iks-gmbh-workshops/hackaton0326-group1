import { notFound } from "next/navigation";
import { getTermsDocument } from "@/lib/terms-content";

type TermsPageProps = {
  params: Promise<{
    slug: string;
  }>;
};

export function generateStaticParams() {
  return [{ slug: "drumdibum-agb-2026-03" }];
}

export default async function TermsPage({ params }: TermsPageProps) {
  const { slug } = await params;
  const document = getTermsDocument(slug);

  if (!document) {
    notFound();
  }

  return (
    <main className="page-shell">
      <div className="page-container max-w-4xl">
        <section className="brand-card p-6 sm:p-8">
          <div className="section-intro">
            <p className="section-title">Rechtliches</p>
            <h1 className="section-headline">{document.title}</h1>
            <p className="subheadline">
              {document.updatedAtLabel} · Version {document.version}
            </p>
            <p className="body-copy">{document.intro}</p>
          </div>

          <div className="brand-divider my-6" />

          <div className="space-y-6">
            {document.sections.map((section) => (
              <section key={section.heading} className="space-y-3">
                <h2 className="subsection-title text-base tracking-[0.14em]">{section.heading}</h2>
                {section.paragraphs.map((paragraph) => (
                  <p key={paragraph} className="body-copy text-[0.98rem]">
                    {paragraph}
                  </p>
                ))}
              </section>
            ))}
          </div>
        </section>
      </div>
    </main>
  );
}
