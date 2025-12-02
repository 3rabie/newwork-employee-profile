import { MemoryRouter, Route, Routes } from "react-router-dom";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from "vitest";
import { AuthProvider } from "../../../auth/contexts/AuthContext";
import type { CoworkerDTO } from "../../types";

const mockGetCoworkerDirectory = vi.fn();

vi.mock("../../api/directoryApi", () => ({
  getCoworkerDirectory: (...args: unknown[]) => mockGetCoworkerDirectory(...args),
}));

let DirectoryPage: (props: unknown) => JSX.Element;

beforeAll(async () => {
  const module = await import("../DirectoryPage");
  DirectoryPage = module.DirectoryPage;
});

afterEach(() => {
  sessionStorage.clear();
});

describe("DirectoryPage", () => {
  const coworkers: CoworkerDTO[] = [
    {
      userId: "user-1",
      employeeId: "ENG-101",
      preferredName: "Alice",
      legalFirstName: "Alice",
      legalLastName: "Nguyen",
      jobTitle: "Senior Engineer",
      department: "Engineering",
      workLocationType: "REMOTE",
      profilePhotoUrl: null,
      relationship: "MANAGER",
      directReport: true,
    },
    {
      userId: "user-2",
      employeeId: "PM-200",
      preferredName: "Brooke",
      legalFirstName: "Brooke",
      legalLastName: "Brown",
      jobTitle: "Product Manager",
      department: "Product",
      workLocationType: "HYBRID",
      profilePhotoUrl: null,
      relationship: "OTHER",
      directReport: false,
    },
  ];

  const renderPage = () =>
    render(
      <MemoryRouter initialEntries={["/people"]}>
        <AuthProvider>
          <Routes>
            <Route path="/people" element={<DirectoryPage />} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    );

  beforeEach(() => {
    vi.clearAllMocks();
    sessionStorage.setItem("auth_token", "token");
    sessionStorage.setItem(
      "auth_user",
      JSON.stringify({
        userId: "manager-1",
        email: "manager@test.com",
        employeeId: "MGR-001",
        role: "MANAGER",
        managerId: null,
      })
    );
  });

  it("renders coworker cards and opens feedback modal", async () => {
    mockGetCoworkerDirectory.mockResolvedValue(coworkers);

    renderPage();

    await screen.findByText(/Product Manager.*Product/i);

    const user = userEvent.setup();
    await user.click(screen.getAllByRole("button", { name: /give feedback/i })[0]);

    expect(await screen.findByText(/recipient:/i)).toHaveTextContent("Alice");
  });

  it("applies filters and calls API with parameters", async () => {
    mockGetCoworkerDirectory
      .mockResolvedValueOnce(coworkers)
      .mockResolvedValueOnce([coworkers[1]]);

    renderPage();
    await screen.findByText(/Senior Engineer.*Engineering/i);

    const user = userEvent.setup();
    await user.type(screen.getByPlaceholderText(/name, email/i), "product");
    await user.selectOptions(screen.getByRole("combobox", { name: /department/i }), "Product");
    await user.click(screen.getByRole("button", { name: /apply filters/i }));

    await waitFor(() => expect(mockGetCoworkerDirectory).toHaveBeenCalledTimes(2));
    expect(mockGetCoworkerDirectory).toHaveBeenLastCalledWith({
      search: "product",
      department: "Product",
      directReportsOnly: undefined,
    });
  });
});
