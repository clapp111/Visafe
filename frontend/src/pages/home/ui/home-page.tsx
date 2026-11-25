"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { LookupForm } from "@/features/lookup-contract";
import { routes } from "@/shared/config";
import { useCurrentLanguage } from "@/shared/i18n";
import {
  ArrowRightIcon,
  Badge,
  CheckIcon,
  InfoIcon,
  MinusCircleIcon,
  Table,
} from "@/shared/ui";
import { HOME_LABELS } from "../model/labels";

export function HomePage() {
  const labels = HOME_LABELS[useCurrentLanguage()];
  const router = useRouter();
  const quickLinks = [
    { href: routes.contractNew, ...labels.quickLinks.employer },
    { href: routes.contracts, ...labels.quickLinks.worker },
  ];

  return (
    <>
      <section
        aria-labelledby="search-title"
        className="border-b border-divider bg-surface-muted"
      >
        <div className="mx-auto max-w-[1200px] px-6 pt-6 pb-14">
          <h1
            id="search-title"
            className="mb-3 text-5xl font-bold tracking-[-0.5px]"
          >
            {labels.title}
          </h1>
          <p className="mb-8 max-w-[760px] text-lg text-muted">
            {labels.description}
          </p>

          <LookupForm
            id="home-confirmation-no"
            hint={labels.lookupHint}
            onSubmit={(confirmationNo) =>
              router.push(routes.lookup(confirmationNo))
            }
            labels={labels.lookupForm}
            className="max-w-[880px] rounded-lg border border-border bg-surface p-8"
          >
            <div className="mt-6 flex flex-wrap items-center gap-4 border-t border-divider pt-6">
              <InfoIcon className="shrink-0 text-primary" />
              <span className="min-w-0 flex-[1_1_300px] text-sm text-muted">
                {labels.newNumberNotice}
              </span>
            </div>
          </LookupForm>
        </div>
      </section>

      <section
        aria-labelledby="quick-title"
        className="mx-auto max-w-[1200px] px-6 pt-14"
      >
        <h2 id="quick-title" className="mb-2 text-4xl leading-normal font-bold">
          {labels.quickTitle}
        </h2>
        <p className="mb-6 text-muted">{labels.quickDescription}</p>
        <div className="flex flex-wrap gap-6">
          {quickLinks.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className="flex min-w-0 flex-[1_1_300px] flex-col gap-3 rounded-lg border border-border bg-surface p-7 text-foreground no-underline"
            >
              <span className="text-sm font-bold text-primary">
                {link.role}
              </span>
              <span className="text-2xl font-bold">{link.title}</span>
              <span className="text-muted">{link.description}</span>
              <span className="mt-auto flex items-center gap-1.5 font-medium text-primary">
                {link.action}
                <ArrowRightIcon />
              </span>
            </Link>
          ))}
        </div>
      </section>

      <section
        aria-labelledby="flow-title"
        className="mx-auto max-w-[1200px] px-6 pt-16"
      >
        <h2 id="flow-title" className="mb-6 text-4xl leading-normal font-bold">
          {labels.flowTitle}
        </h2>
        <ol className="flex flex-wrap gap-4">
          {labels.flowSteps.map((step) => (
            <li
              key={step.step}
              className="min-w-0 flex-[1_1_220px] border-t-4 border-primary bg-surface-muted p-5"
            >
              <span className="block text-sm font-bold text-primary">
                {step.step}
              </span>
              <strong className="mt-1 mb-1.5 block text-lg">
                {step.title}
              </strong>
              <span className="text-sm text-muted">{step.description}</span>
            </li>
          ))}
        </ol>
      </section>

      <section
        aria-labelledby="status-title"
        className="mx-auto max-w-[1200px] px-6 pt-16 pb-18"
      >
        <h2
          id="status-title"
          className="mb-2 text-4xl leading-normal font-bold"
        >
          {labels.statusTitle}
        </h2>
        <p className="mb-6 text-muted">{labels.statusDescription}</p>
        <Table
          caption={labels.statusCaption}
          className="min-w-[640px] table-fixed"
        >
          <thead>
            <tr>
              <th scope="col" className="w-32">
                {labels.statusColumn}
              </th>
              <th scope="col">{labels.meaningColumn}</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>
                <Badge tone="success" size="md" icon={<CheckIcon size={16} />}>
                  {labels.confirmed}
                </Badge>
              </td>
              <td>{labels.confirmedMeaning}</td>
            </tr>
            <tr>
              <td>
                <Badge
                  tone="neutral"
                  size="md"
                  icon={<MinusCircleIcon size={16} />}
                >
                  {labels.terminated}
                </Badge>
              </td>
              <td>{labels.terminatedMeaning}</td>
            </tr>
          </tbody>
        </Table>
      </section>
    </>
  );
}
